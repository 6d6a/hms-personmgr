package ru.majordomo.hms.personmgr.controller.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestWrapper;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import ru.majordomo.hms.personmgr.common.MailManagerMessageType;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.event.accountHistory.AccountHistoryEvent;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.manager.AccountAbonementManager;
import ru.majordomo.hms.personmgr.model.abonement.AccountAbonement;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.model.service.AccountService;
import ru.majordomo.hms.personmgr.model.service.PaymentService;
import ru.majordomo.hms.personmgr.repository.AccountServiceRepository;
import ru.majordomo.hms.personmgr.repository.PaymentServiceRepository;
import ru.majordomo.hms.personmgr.repository.PlanRepository;
import ru.majordomo.hms.personmgr.service.AccountHelper;
import ru.majordomo.hms.personmgr.service.AccountServiceHelper;
import ru.majordomo.hms.personmgr.validation.ObjectId;

import static ru.majordomo.hms.personmgr.common.Constants.ANTI_SPAM_SERVICE_ID;
import static ru.majordomo.hms.personmgr.common.Constants.ENABLED_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.HISTORY_MESSAGE_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.OPERATOR_KEY;
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

    @Autowired
    public AccountServiceRestController(
            AccountServiceRepository accountServiceRepository,
            PaymentServiceRepository serviceRepository,
            AccountServiceHelper accountServiceHelper,
            AccountHelper accountHelper,
            AccountAbonementManager accountAbonementManager,
            PlanRepository planRepository
    ) {
        this.accountServiceRepository = accountServiceRepository;
        this.serviceRepository = serviceRepository;
        this.accountServiceHelper = accountServiceHelper;
        this.accountHelper = accountHelper;
        this.accountAbonementManager = accountAbonementManager;
        this.planRepository = planRepository;
    }

    @RequestMapping(value = "/{accountId}/account-service/{accountServiceId}",
                    method = RequestMethod.GET)
    public ResponseEntity<AccountService> get(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @ObjectId(AccountService.class) @PathVariable(value = "accountServiceId") String accountServiceId
    ) {
        PersonalAccount account = accountManager.findOne(accountId);

        AccountService accountService = accountServiceRepository.findByPersonalAccountIdAndId(account.getId(), accountServiceId);

        if (accountService == null) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity<>(accountService, HttpStatus.OK);
    }

    @RequestMapping(value = "/{accountId}/account-service",
                    method = RequestMethod.GET)
    public ResponseEntity<Page<AccountService>> getAll(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            Pageable pageable
    ) {
        PersonalAccount account = accountManager.findOne(accountId);

        Page<AccountService> accountServices = accountServiceRepository.findByPersonalAccountId(account.getId(), pageable);

        return new ResponseEntity<>(accountServices, HttpStatus.OK);
    }

    @PreAuthorize("hasAuthority('MANAGE_SERVICES')")
    @RequestMapping(value = "/{accountId}/account-service",
                    method = RequestMethod.POST)
    public ResponseEntity<SimpleServiceMessage> addService(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @RequestBody Map<String, Object> requestBody,
            SecurityContextHolderAwareRequestWrapper request
    ) {
        PersonalAccount account = accountManager.findOne(accountId);

        checkRequiredParams(requestBody, ACCOUNT_SERVICE_CREATE);

        String paymentServiceId = (String) requestBody.get("paymentServiceId");

        PaymentService paymentService = getPaymentServiceById(paymentServiceId);

        if (accountServiceHelper.accountHasService(account, paymentServiceId)) {
            throw new ParameterValidationException("accountService already found for specified paymentServiceId " +
                    paymentServiceId);
        }

        //Сейчас баланс проверяется по полной стоимости услуги
        accountHelper.checkBalance(account, paymentService);

        accountHelper.charge(account, paymentService);

        accountServiceHelper.addAccountService(account, paymentServiceId);

        //Save history
        String operator = request.getUserPrincipal().getName();
        Map<String, String> params = new HashMap<>();
        params.put(HISTORY_MESSAGE_KEY, "Произведен заказ услуги " + paymentService.getName());
        params.put(OPERATOR_KEY, operator);

        publisher.publishEvent(new AccountHistoryEvent(accountId, params));

        return new ResponseEntity<>(this.createSuccessResponse("accountService created with id " + paymentServiceId), HttpStatus.OK);
    }

    @RequestMapping(value = "/{accountId}/account-service-sms-notification",
                    method = RequestMethod.GET)
    public ResponseEntity<AccountService> getSmsService(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId
    ) {
        PersonalAccount account = accountManager.findOne(accountId);

        PaymentService paymentService = accountServiceHelper.getSmsPaymentServiceByPlanId(account.getPlanId());

        List<AccountService> accountServices = accountServiceRepository.findByPersonalAccountIdAndServiceId(account.getId(), paymentService.getId());

        if (accountServices == null || accountServices.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } else if (accountServices.size() > 1) {
            throw new ParameterValidationException("Account has more than one AccountService with serviceId " + paymentService.getId());
        }

        return new ResponseEntity<>(accountServices.get(0), HttpStatus.OK);
    }

    @RequestMapping(value = "/{accountId}/account-service-sms-notification",
                    method = RequestMethod.POST)
    public ResponseEntity<SimpleServiceMessage> addSmsService(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @RequestBody Map<String, Object> requestBody,
            SecurityContextHolderAwareRequestWrapper request
    ) {
        PersonalAccount account = accountManager.findOne(accountId);

        checkRequiredParams(requestBody, ACCOUNT_SERVICE_ENABLE);

        Boolean enabled = (Boolean) requestBody.get(ENABLED_KEY);

        if (enabled) {
            Set<MailManagerMessageType> smsNotifications = account.getNotifications().stream()
                    .filter(mailManagerMessageType -> mailManagerMessageType.name().startsWith("SMS_"))
                    .collect(Collectors.toSet());

            boolean smsNotificationsEmpty = smsNotifications.isEmpty();
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

        //Save history
        String operator = request.getUserPrincipal().getName();
        Map<String, String> params = new HashMap<>();
        params.put(HISTORY_MESSAGE_KEY, "Произведено " + (enabled ? "включение" : "отключение") + " услуги " + paymentService.getName());
        params.put(OPERATOR_KEY, operator);

        publisher.publishEvent(new AccountHistoryEvent(accountId, params));

        return new ResponseEntity<>(this.createSuccessResponse("accountService " + (enabled ? "enabled" : "disabled") + " for sms-notification"), HttpStatus.OK);
    }

    @RequestMapping(value = "/{accountId}/account-service-anti-spam",
                    method = RequestMethod.GET)
    public ResponseEntity<AccountService> getAntiSpamService(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId
    ) {
        PersonalAccount account = accountManager.findOne(accountId);

        PaymentService paymentService = getPaymentServiceByOldId(ANTI_SPAM_SERVICE_ID);

        List<AccountService> accountServices = accountServiceRepository.findByPersonalAccountIdAndServiceId(account.getId(), paymentService.getId());

        if (accountServices == null || accountServices.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } else if (accountServices.size() > 1) {
            throw new ParameterValidationException("Account has more than one AccountService with serviceId " + paymentService.getId());
        }

        return new ResponseEntity<>(accountServices.get(0), HttpStatus.OK);
    }

    @RequestMapping(value = "/{accountId}/account-service-anti-spam",
                    method = RequestMethod.POST)
    public ResponseEntity<SimpleServiceMessage> addAntiSpamService(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @RequestBody Map<String, Object> requestBody,
            SecurityContextHolderAwareRequestWrapper request
    ) {
        PersonalAccount account = accountManager.findOne(accountId);

        checkRequiredParams(requestBody, ACCOUNT_SERVICE_ENABLE);

        Boolean enabled = (Boolean) requestBody.get(ENABLED_KEY);

        PaymentService paymentService = getPaymentServiceByOldId(ANTI_SPAM_SERVICE_ID);

        processCustomService(account, paymentService, enabled);

        //Save history
        String operator = request.getUserPrincipal().getName();
        Map<String, String> params = new HashMap<>();
        params.put(HISTORY_MESSAGE_KEY, "Произведено " + (enabled ? "включение" : "отключение") + " услуги " + paymentService.getName());
        params.put(OPERATOR_KEY, operator);

        publisher.publishEvent(new AccountHistoryEvent(accountId, params));

        return new ResponseEntity<>(this.createSuccessResponse("accountService " + (enabled ? "enabled" : "disabled") + " for anti-spam"), HttpStatus.OK);
    }

    @PreAuthorize("hasAuthority('MANAGE_SERVICES')")
    @RequestMapping(value = "/{accountId}/account-service/{accountServiceId}",
                    method = RequestMethod.DELETE)
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
            //Save history
            String operator = request.getUserPrincipal().getName();
            Map<String, String> params = new HashMap<>();
            params.put(HISTORY_MESSAGE_KEY, "Произведено удаление услуги '" + serviceName + "', id: " + accountServiceId);
            params.put(OPERATOR_KEY, operator);

            publisher.publishEvent(new AccountHistoryEvent(accountId, params));
        }

        return new ResponseEntity<>(HttpStatus.OK);
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
            throw new ParameterValidationException("paymentService with oldId " + paymentServiceOldId + " not found");
        }

        return paymentService;
    }

    private void processCustomService(PersonalAccount account, PaymentService paymentService, Boolean enable) {
        if (enable) {
            if (accountServiceHelper.accountHasService(account, paymentService.getId())) {
                List<AccountService> accountServices = accountServiceHelper.getAccountServices(account, paymentService.getId());

                if (accountServices.size() > 1) {
                    throw new ParameterValidationException("Account has more than one AccountService with serviceId " + paymentService.getId());
                }

                //включаем и при необходимости списываем бабло
                accountServiceHelper.enableAccountService(account, paymentService.getId());

                AccountService currentService = accountServices.get(0);

                if (currentService.getLastBilled() == null || currentService.getLastBilled().toLocalDate().compareTo(LocalDate.now()) < 0) {
                    accountHelper.checkBalance(account, paymentService, true);

                    BigDecimal dayCost = accountHelper.getDayCostByService(paymentService);

                    accountHelper.charge(account, paymentService, dayCost);

                    accountServiceHelper.setLastBilledAccountService(account, paymentService.getId());
                }
            } else {
                //добавляем услугу, списываем деньги
                accountHelper.checkBalance(account, paymentService, true);

                BigDecimal dayCost = accountHelper.getDayCostByService(paymentService);

                accountHelper.charge(account, paymentService, dayCost);

                accountServiceHelper.addAccountService(account, paymentService.getId());

                accountServiceHelper.setLastBilledAccountService(account, paymentService.getId());
            }
        } else {
            if (accountServiceHelper.accountHasService(account, paymentService.getId())) {
                List<AccountService> accountServices = accountServiceHelper.getAccountServices(account, paymentService.getId());

                if (accountServices.size() > 1) {
                    throw new ParameterValidationException("Account has more than one AccountService with serviceId " + paymentService.getId());
                }
                //выключаем
                accountServiceHelper.disableAccountService(account, paymentService.getId());
            }
        }
    }
}