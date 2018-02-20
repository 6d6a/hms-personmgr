package ru.majordomo.hms.personmgr.controller.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestWrapper;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import ru.majordomo.hms.personmgr.common.Utils;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.manager.AccountAbonementManager;
import ru.majordomo.hms.personmgr.model.abonement.AccountAbonement;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.model.service.AccountService;
import ru.majordomo.hms.personmgr.model.service.DiscountedService;
import ru.majordomo.hms.personmgr.model.service.PaymentService;
import ru.majordomo.hms.personmgr.repository.AccountServiceRepository;
import ru.majordomo.hms.personmgr.repository.PaymentServiceRepository;
import ru.majordomo.hms.personmgr.repository.PlanRepository;
import ru.majordomo.hms.personmgr.service.*;
import ru.majordomo.hms.personmgr.validation.ObjectId;

import static ru.majordomo.hms.personmgr.common.Constants.ANTI_SPAM_SERVICE_ID;
import static ru.majordomo.hms.personmgr.common.Constants.ENABLED_KEY;
import static ru.majordomo.hms.personmgr.common.PhoneNumberManager.phoneValid;
import static ru.majordomo.hms.personmgr.common.RequiredField.ACCOUNT_SERVICE_CREATE;
import static ru.majordomo.hms.personmgr.common.RequiredField.ACCOUNT_SERVICE_ENABLE;


@RestController
@Validated
public class AccountServiceRestController extends CommonRestController {

    private final AccountServiceRepository accountServiceRepository;
    private final PaymentServiceRepository serviceRepository;
    private final AccountServiceHelper accountServiceHelper;
    private final AccountHelper accountHelper;
    private final AccountAbonementManager accountAbonementManager;
    private final PlanRepository planRepository;
    private final AccountNotificationHelper accountNotificationHelper;
    private final DiscountServiceHelper discountServiceHelper;

    @Autowired
    public AccountServiceRestController(
            AccountServiceRepository accountServiceRepository,
            PaymentServiceRepository serviceRepository,
            AccountServiceHelper accountServiceHelper,
            AccountHelper accountHelper,
            AccountAbonementManager accountAbonementManager,
            PlanRepository planRepository,
            AccountNotificationHelper accountNotificationHelper,
            DiscountServiceHelper discountServiceHelper
    ) {
        this.accountServiceRepository = accountServiceRepository;
        this.serviceRepository = serviceRepository;
        this.accountServiceHelper = accountServiceHelper;
        this.accountHelper = accountHelper;
        this.accountAbonementManager = accountAbonementManager;
        this.planRepository = planRepository;
        this.accountNotificationHelper = accountNotificationHelper;
        this.discountServiceHelper = discountServiceHelper;
    }

    @GetMapping(value = "/{accountId}/account-service/{accountServiceId}")
    public ResponseEntity<AccountService> get(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @ObjectId(AccountService.class) @PathVariable(value = "accountServiceId") String accountServiceId
    ) {
        AccountService accountService = accountServiceRepository.findByPersonalAccountIdAndId(accountId, accountServiceId);

        if (accountService == null) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity<>(accountService, HttpStatus.OK);
    }

    @GetMapping("/{accountId}/account-service")
    public ResponseEntity<Page<AccountService>> getAll(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            Pageable pageable
    ) {
        Page<AccountService> accountServices = accountServiceRepository.findByPersonalAccountId(accountId, pageable);

        return new ResponseEntity<>(accountServices, HttpStatus.OK);
    }

    @PreAuthorize("hasAuthority('MANAGE_SERVICES')")
    @PostMapping("/{accountId}/account-service")
    public ResponseEntity<AccountService> addService(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @RequestBody Map<String, Object> requestBody,
            SecurityContextHolderAwareRequestWrapper request
    ) {
        PersonalAccount account = accountManager.findOne(accountId);

        Utils.checkRequiredParams(requestBody, ACCOUNT_SERVICE_CREATE);

        String paymentServiceId = (String) requestBody.get("paymentServiceId");

        PaymentService paymentService = getPaymentServiceById(paymentServiceId);

        if (accountServiceHelper.accountHasService(account, paymentServiceId)) {
            throw new ParameterValidationException("На аккаунте уже есть услуга '" + paymentService.getName() + "'");
        }

        accountHelper.checkBalance(account, paymentService);

        ChargeMessage chargeMessage = new ChargeMessage.Builder(paymentService).build();

        accountHelper.charge(account, chargeMessage);

        AccountService accountService = accountServiceHelper.addAccountService(account, paymentServiceId);

        accountHelper.saveHistory(account, "Произведен заказ услуги " + paymentService.getName(), request);

        return new ResponseEntity<>(accountService, HttpStatus.OK);
    }

    @GetMapping("/{accountId}/account-service-sms-notification")
    public ResponseEntity<AccountService> getSmsService(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId
    ) {
        PersonalAccount account = accountManager.findOne(accountId);

        PaymentService paymentService = accountServiceHelper.getSmsPaymentServiceByPlanId(account.getPlanId());

        List<AccountService> accountServices = accountServiceRepository.findByPersonalAccountIdAndServiceId(account.getId(), paymentService.getId());

        if (accountServices == null || accountServices.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } else if (accountServices.size() > 1) {
            throw new ParameterValidationException(
                    "На аккаунте обнаружено больше одной услуги '" + paymentService.getName()
                            + "'. Пожалуйста, обратитесь в финансовый отдел.");}

        return new ResponseEntity<>(accountServices.get(0), HttpStatus.OK);
    }

    @PostMapping("/{accountId}/account-service-sms-notification")
    public ResponseEntity<SimpleServiceMessage> addSmsService(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @RequestBody Map<String, Object> requestBody,
            SecurityContextHolderAwareRequestWrapper request
    ) {
        PersonalAccount account = accountManager.findOne(accountId);

        Utils.checkRequiredParams(requestBody, ACCOUNT_SERVICE_ENABLE);

        Boolean enabled = (Boolean) requestBody.get(ENABLED_KEY);

        if (enabled) {

            boolean smsNotificationsEmpty = !accountNotificationHelper.hasActiveSmsNotifications(account);

            boolean phoneInvalid = account.getSmsPhoneNumber() == null || !phoneValid(account.getSmsPhoneNumber());
            if (smsNotificationsEmpty || phoneInvalid) {
                String message;
                if (smsNotificationsEmpty && phoneInvalid) {
                    message = "Выберите хотя бы один вид уведомлений и укажите корректный номер телефона.";
                } else if (smsNotificationsEmpty) {
                    message = "Выберите хотя бы один вид уведомлений.";
                } else {
                    message = "Укажите корректный номер телефона.";
                }
                throw new ParameterValidationException(message);
            }
        }

        PaymentService paymentService = accountServiceHelper.getSmsPaymentServiceByPlanId(account.getPlanId());

        processCustomService(account, paymentService, enabled);

        accountHelper.saveHistory(
                account,
                "Произведено " + (enabled ? "включение" : "отключение") + " услуги " + paymentService.getName(),
                request
        );

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping("/{accountId}/account-service-anti-spam")
    public ResponseEntity<AccountService> getAntiSpamService(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId
    ) {
        PersonalAccount account = accountManager.findOne(accountId);

        PaymentService paymentService = getPaymentServiceByOldId(ANTI_SPAM_SERVICE_ID);

        List<AccountService> accountServices = accountServiceRepository.findByPersonalAccountIdAndServiceId(account.getId(), paymentService.getId());

        if (accountServices == null || accountServices.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } else if (accountServices.size() > 1) {
            throw new ParameterValidationException(
                    "На аккаунте обнаружено больше одной услуги '" + paymentService.getName()
                            + "'. Пожалуйста, обратитесь в финансовый отдел на почту billing@majordomo.ru");
        }

        return new ResponseEntity<>(accountServices.get(0), HttpStatus.OK);
    }

    @PostMapping("/{accountId}/account-service-anti-spam")
    public ResponseEntity<SimpleServiceMessage> addAntiSpamService(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @RequestBody Map<String, Object> requestBody,
            SecurityContextHolderAwareRequestWrapper request
    ) {
        PersonalAccount account = accountManager.findOne(accountId);

        Utils.checkRequiredParams(requestBody, ACCOUNT_SERVICE_ENABLE);

        Boolean enabled = (Boolean) requestBody.get(ENABLED_KEY);

        PaymentService paymentService = getPaymentServiceByOldId(ANTI_SPAM_SERVICE_ID);

        processCustomService(account, paymentService, enabled);

        accountHelper.switchAntiSpamForMailboxes(account, enabled);

        accountHelper.saveHistory(
                account,
                "Произведено " + (enabled ? "включение" : "отключение") + " услуги " + paymentService.getName(),
                request
        );

        return new ResponseEntity<>(this.createSuccessResponse("accountService " + (enabled ? "enabled" : "disabled") + " for anti-spam"), HttpStatus.OK);
    }

    @PreAuthorize("hasAuthority('MANAGE_SERVICES')")
    @DeleteMapping("/{accountId}/account-service/{accountServiceId}")
    public ResponseEntity<Object> delete(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @ObjectId(AccountService.class) @PathVariable(value = "accountServiceId") String accountServiceId,
            SecurityContextHolderAwareRequestWrapper request
    ) {
        PersonalAccount account = accountManager.findOne(accountId);

        AccountService accountService = accountServiceRepository.findByPersonalAccountIdAndId(account.getId(), accountServiceId);

        String serviceName;

        if (accountService != null) {
            serviceName = accountService.getName();
        } else {
            throw new ParameterValidationException("Услуга с Id " + accountServiceId + " не найдена на аккаунте " + accountId);
        }

        AccountAbonement currentAccountAbonement = accountAbonementManager.findByPersonalAccountId(account.getId());

        if (currentAccountAbonement == null || (currentAccountAbonement.getExpired() != null
                && currentAccountAbonement.getExpired().isBefore(LocalDateTime.now()))) {
            List<AccountService> accountServices = accountServiceRepository.findByPersonalAccountId(account.getId());

            Plan plan = planRepository.findOne(account.getPlanId());

            accountServices = accountServices
                    .stream()
                    .filter(accountService1 -> !accountService1.getId().equals(accountServiceId))
                    .collect(Collectors.toList())
            ;

            if (accountServices
                    .stream()
                    .noneMatch(accountService1 -> accountService1.getServiceId().equals(plan.getServiceId()))
                    ) {
                throw new ParameterValidationException("Услуга с Id " + accountServiceId +
                        " является 'Тарифным планом' для аккаунта '"  + accountId +
                        "' и не может быть удалена если на аккаунте нет абонемента");
            }
        }

        accountServiceHelper.deleteAccountServiceById(account, accountServiceId);

        if (serviceName != null) {
            accountHelper.saveHistory(
                    accountId, "Произведено удаление услуги '" + serviceName + "', id: " + accountServiceId, request);
        }

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    private PaymentService getPaymentServiceById(String paymentServiceId) {
        PaymentService paymentService = serviceRepository.findOne(paymentServiceId);

        if (paymentService == null) {
            throw new ParameterValidationException("paymentService with id " + paymentServiceId + " not found");
        }

        return paymentService;
    }

    private PaymentService getPaymentServiceByOldId(String paymentServiceOldId) {
        PaymentService paymentService = serviceRepository.findByOldId(paymentServiceOldId);

        if (paymentService == null) {
            throw new ParameterValidationException("Не найдена услуга с oldId " + paymentServiceOldId);
        }

        return paymentService;
    }

    private void processCustomService(PersonalAccount account, PaymentService paymentService, Boolean enable) {
        boolean accountHasService = accountServiceHelper.accountHasService(account, paymentService.getId());
        if (enable) {
            enableCustomService(account, paymentService, accountHasService);
        } else {
            if (accountHasService) {
                disableCustomService(account, paymentService);
            }
        }
    }

    private void enableCustomService(PersonalAccount account, PaymentService paymentService, boolean accountHasService){
        if (accountHasService) {

            AccountService currentService = getAccountServiceByPaymentServiceId(account, paymentService.getId());

            if (currentService.getLastBilled() == null || currentService.getLastBilled().toLocalDate().compareTo(LocalDate.now()) < 0) {
                chargeMoneyAndAddService(account, paymentService, accountHasService);
            }
            accountServiceHelper.enableAccountService(account, paymentService.getId());

        } else {
            chargeMoneyAndAddService(account, paymentService, accountHasService);
        }
    }

    private void disableCustomService(PersonalAccount account, PaymentService paymentService){
        checkAccountHasOneServiceWithId(account, paymentService.getId());
        accountServiceHelper.disableAccountService(account, paymentService.getId());
    }

    private void chargeMoneyAndAddService(PersonalAccount account, PaymentService paymentService, boolean accountHasService) {
        accountHelper.checkBalance(account, paymentService, true);

        BigDecimal dayCost = this.getDailyCost(account, paymentService);

        if (dayCost.compareTo(BigDecimal.ZERO) > 0) {
            logger.debug("account [" + account + "] dailyCost for service [" + paymentService + "] = " + dayCost);
            ChargeMessage chargeMessage = new ChargeMessage.Builder(paymentService)
                    .setAmount(dayCost)
                    .build();
            accountHelper.charge(account, chargeMessage);
        } else {
            logger.debug("account [" + account + "] dailyCost for service [" + paymentService + "] <= 0 ");
        }

        if (!accountHasService) { accountServiceHelper.addAccountService(account, paymentService.getId()); }

        accountServiceHelper.setLastBilledAccountService(account, paymentService.getId());
    }

    private AccountService getAccountServiceByPaymentServiceId(PersonalAccount account, String paymentServiceId) {
        List<AccountService> accountServices = accountServiceHelper.getAccountServices(account, paymentServiceId);
        if (accountServices.size() > 1) {
            throw new ParameterValidationException(
                    "На аккаунте обнаружено больше одной услуги с id '" + paymentServiceId
                            + "'. Пожалуйста, обратитесь в финансовый отдел.");
        }
        return accountServices.get(0);
    }

    private void checkAccountHasOneServiceWithId(PersonalAccount account, String paymentServiceId) {
        List<AccountService> accountServices = accountServiceHelper.getAccountServices(account, paymentServiceId);
        if (accountServices.size() > 1) {
            throw new ParameterValidationException(
                    "На аккаунте обнаружено больше одной услуги с id '" + paymentServiceId
                            + "'. Пожалуйста, обратитесь в финансовый отдел.");
        }
    }

    private BigDecimal getDailyCost(PersonalAccount account, PaymentService paymentService) {
        BigDecimal dayCost;

        try {
            DiscountedService discountedService = discountServiceHelper.getDiscountedService(account.getDiscounts(), paymentService);
            if (discountedService != null) {
                dayCost = accountServiceHelper.getDailyCostForService(discountedService);
            } else {
                dayCost = accountHelper.getDayCostByService(paymentService);
            }
        } catch (Exception e) {
            logger.error("Catch exception with getCost in AccountServiceRestController for paymentService " + paymentService + " and account " + account);
            e.printStackTrace();
            dayCost = accountHelper.getDayCostByService(paymentService);
        }
        return dayCost;
    }
}