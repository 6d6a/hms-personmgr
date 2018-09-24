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

import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.event.account.UserDisabledServiceEvent;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.exception.ResourceNotFoundException;
import ru.majordomo.hms.personmgr.manager.AbonementManager;
import ru.majordomo.hms.personmgr.manager.PlanManager;
import ru.majordomo.hms.personmgr.model.abonement.AccountAbonement;
import ru.majordomo.hms.personmgr.model.abonement.AccountServiceAbonement;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.plan.Feature;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.model.plan.ServicePlan;
import ru.majordomo.hms.personmgr.model.revisium.RevisiumRequestService;
import ru.majordomo.hms.personmgr.model.service.AccountService;
import ru.majordomo.hms.personmgr.model.service.DiscountedService;
import ru.majordomo.hms.personmgr.model.service.PaymentService;
import ru.majordomo.hms.personmgr.model.service.RedirectAccountService;
import ru.majordomo.hms.personmgr.repository.*;
import ru.majordomo.hms.personmgr.service.*;
import ru.majordomo.hms.personmgr.validation.ObjectId;

import static ru.majordomo.hms.personmgr.common.PhoneNumberManager.phoneValid;


@RestController
@Validated
public class AccountServiceRestController extends CommonRestController {

    private final AccountServiceHelper accountServiceHelper;
    private final AccountHelper accountHelper;
    private final AbonementManager<AccountAbonement> accountAbonementManager;
    private final AbonementManager<AccountServiceAbonement> accountServiceAbonementManager;
    private final PlanManager planManager;
    private final AccountNotificationHelper accountNotificationHelper;
    private final DiscountServiceHelper discountServiceHelper;
    private final ServiceAbonementService serviceAbonementService;
    private final AccountRedirectServiceRepository redirectServiceRepository;
    private final RevisiumRequestServiceRepository revisiumRequestServiceRepository;

    @Autowired
    public AccountServiceRestController(
            AccountServiceHelper accountServiceHelper,
            AccountHelper accountHelper,
            AbonementManager<AccountAbonement> accountAbonementManager,
            AbonementManager<AccountServiceAbonement> accountServiceAbonementManager,
            PlanManager planManager,
            AccountNotificationHelper accountNotificationHelper,
            DiscountServiceHelper discountServiceHelper,
            ServiceAbonementService serviceAbonementService,
            AccountRedirectServiceRepository redirectServiceRepository,
            RevisiumRequestServiceRepository revisiumRequestServiceRepository
    ) {
        this.accountServiceHelper = accountServiceHelper;
        this.accountHelper = accountHelper;
        this.accountAbonementManager = accountAbonementManager;
        this.planManager = planManager;
        this.accountNotificationHelper = accountNotificationHelper;
        this.discountServiceHelper = discountServiceHelper;
        this.serviceAbonementService = serviceAbonementService;
        this.accountServiceAbonementManager = accountServiceAbonementManager;
        this.redirectServiceRepository = redirectServiceRepository;
        this.revisiumRequestServiceRepository = revisiumRequestServiceRepository;
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

    @GetMapping("/{accountId}/account-service-abonement")
    public ResponseEntity<Page<AccountServiceAbonement>> getAllServiceAbonements(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            Pageable pageable
    ) {
        Page<AccountServiceAbonement> accountServices = serviceAbonementRepository.findByPersonalAccountId(accountId, pageable);

        return new ResponseEntity<>(accountServices, HttpStatus.OK);
    }

    @PreAuthorize("hasAuthority('MANAGE_SERVICES')")
    @DeleteMapping("/{accountId}/account-service-abonement/{abonementId}")
    public ResponseEntity<AccountServiceAbonement> del(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @ObjectId(AccountServiceAbonement.class) @PathVariable(value = "abonementId") String abonementId,
            SecurityContextHolderAwareRequestWrapper request
    ) {
        AccountServiceAbonement abonement = accountServiceAbonementManager.findByIdAndPersonalAccountId(abonementId, accountId);
        PersonalAccount account = accountManager.findOne(accountId);

        if (abonement == null) {
            throw new ResourceNotFoundException("Абонемент не найден");
        }

        String message = "Удален абонемент на услугу: " + abonement.getAbonement().getName();

        switch (abonement.getAbonement().getType()) {
            case REDIRECT:
                RedirectAccountService redirect = redirectServiceRepository.findByAccountServiceAbonementId(abonementId);
                accountHelper.deleteRedirects(account, redirect.getFullDomainName());
                redirectServiceRepository.delete(redirect);
                message += " домен: " +  redirect.getFullDomainName();

                break;
            case ANTI_SPAM:
                accountHelper.switchAntiSpamForMailboxes(account, false);

                break;
            case REVISIUM:
                RevisiumRequestService revisiumService = revisiumRequestServiceRepository.findByPersonalAccountIdAndAccountServiceAbonementId(
                        accountId, abonementId
                );

                revisiumRequestServiceRepository.delete(revisiumService);

                message += " домен: " + revisiumService.getSiteUrl();

                break;
            case VIRTUAL_HOSTING_PLAN:
                throw new ParameterValidationException(
                        "Обратитесь в отдел разработки! Абонемент на хостинг обнаружен среди абонементов на доп. услуги."
                );
        }

        accountServiceAbonementManager.delete(abonement.getId());

        history.save(accountId, message, request);

        return new ResponseEntity<>(abonement, HttpStatus.OK);
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

            Plan plan = planManager.findOne(account.getPlanId());

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
            history.save(accountId, "Произведено удаление услуги '" + serviceName + "', id: " + accountServiceId, request);
        }

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PostMapping("/{accountId}/account-service")
    public ResponseEntity<SimpleServiceMessage> addService(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @RequestParam(value = "feature") Feature feature,
            @RequestParam(value = "enabled", defaultValue = "true") boolean enabled,
            SecurityContextHolderAwareRequestWrapper request
    ) {
        PersonalAccount account = accountManager.findOne(accountId);

        ServicePlan plan = accountServiceHelper.getServicePlanForFeatureByAccount(feature, account);

        if (feature == Feature.SMS_NOTIFICATIONS && enabled) {
            checkSmsAllowness(account);
        }

        if (plan == null) {
            throw new ParameterValidationException("Услуга " + feature.name() + " не найдена");
        }

        List<AccountServiceAbonement> accountServiceAbonements = serviceAbonementRepository.findByPersonalAccountIdAndAbonementIdIn(
                account.getId(),
                plan.getAbonementIds()
        );

        if (accountServiceAbonements != null && !accountServiceAbonements.isEmpty()) {
            throw new ParameterValidationException("При активном абонементе нельзя " + (enabled ? "включить" : "отключить") + "услугу");
        }

        if (plan.isAbonementOnly() && enabled) {
            throw new ParameterValidationException("Услуга " + feature.name() + " может работать только по абонементу");
        }

        processCustomService(account, plan.getService(), enabled);

        if (feature == Feature.ANTI_SPAM) {
            accountHelper.switchAntiSpamForMailboxes(account, enabled);
        }

        history.save(
                account,
                "Произведено " + (enabled ? "включение" : "отключение") + " услуги " + plan.getService().getName(),
                request
        );

        return new ResponseEntity<>(
                this.createSuccessResponse("accountService " + (enabled ? "enabled" : "disabled") + " for " + feature.name()),
                HttpStatus.OK
        );
    }

    @GetMapping("/{accountId}/account-service/filter")
    public ResponseEntity<List<AccountService>> getService(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @RequestParam(value = "feature") Feature feature
    ) {
        PersonalAccount account = accountManager.findOne(accountId);

        ServicePlan plan = accountServiceHelper.getServicePlanForFeatureByAccount(feature, account);

        if (plan == null) {
            throw new ParameterValidationException("Услуга " + feature.name() + " не найдена");
        }

        List<AccountService> accountServices = accountServiceRepository.findByPersonalAccountIdAndServiceId(
                account.getId(),
                plan.getServiceId()
        );

        if (accountServices == null || accountServices.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } else if (feature.isOnlyOnePerAccount() && accountServices.size() > 1) {
            throw new ParameterValidationException(
                    "На аккаунте обнаружено больше одной услуги '" + plan.getService().getName()
                            + "'. Пожалуйста, обратитесь в финансовый отдел.");}

        return new ResponseEntity<>(accountServices, HttpStatus.OK);
    }

    @GetMapping("/{accountId}/account-service-abonement/filter")
    public ResponseEntity<List<AccountServiceAbonement>> getAbonementService(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @RequestParam(value = "feature") Feature feature
    ) {
        PersonalAccount account = accountManager.findOne(accountId);

        ServicePlan plan = accountServiceHelper.getServicePlanForFeatureByAccount(feature, account);

        if (plan == null) {
            throw new ParameterValidationException("Услуга " + feature.name() + " не найдена");
        }

        List<AccountServiceAbonement> accountServiceAbonements = serviceAbonementRepository.findByPersonalAccountIdAndAbonementIdIn(
                account.getId(),
                plan.getAbonementIds()
        );

        if (accountServiceAbonements == null || accountServiceAbonements.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } else if (feature.isOnlyOnePerAccount() && accountServiceAbonements.size() > 1) {
            throw new ParameterValidationException(
                    "На аккаунте обнаружено больше одного абонемента на услугу '" + plan.getService().getName()
                            + "'. Пожалуйста, обратитесь в финансовый отдел.");}

        return new ResponseEntity<>(accountServiceAbonements, HttpStatus.OK);
    }

    @PostMapping("/{accountId}/account-service-abonement")
    public ResponseEntity<AccountServiceAbonement> addServiceAbonement(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @RequestParam(value = "feature") Feature feature,
            @RequestParam(value = "abonementId", required = false) String abonementId
    ) {
        PersonalAccount account = accountManager.findOne(accountId);

        ServicePlan plan = accountServiceHelper.getServicePlanForFeatureByAccount(feature, account);

        if (feature == Feature.SMS_NOTIFICATIONS) {
            checkSmsAllowness(account);
        }

        if (plan == null) {
            throw new ParameterValidationException("Услуга " + feature.name() + " не найдена");
        }

        if (abonementId != null && (!plan.getAbonementIds().contains(abonementId) || plan.getAbonementById(abonementId).isInternal())) {
            throw new ParameterValidationException("Абонемент на услугу " + feature.name() + " не найден");
        }

        List<AccountService> accountServices = accountServiceRepository.findByPersonalAccountIdAndServiceId(
                account.getId(),
                plan.getServiceId()
        );

        if (accountServices != null && feature.isOnlyOnePerAccount() && accountServices.size() > 1) {
            throw new ParameterValidationException(
                    "На аккаунте обнаружено больше одной услуги '" + plan.getService().getName()
                            + "'. Пожалуйста, обратитесь в финансовый отдел.");
        }

        AccountServiceAbonement accountServiceAbonement = serviceAbonementService.addAbonement(
                account,
                abonementId != null ? abonementId : plan.getNotInternalAbonementId(),
                feature,
                true
        );

        return new ResponseEntity<>(accountServiceAbonement, HttpStatus.OK);
    }

    private void processCustomService(PersonalAccount account, PaymentService paymentService, Boolean enable) {
        boolean accountHasService = accountServiceHelper.accountHasService(account, paymentService.getId());
        if (enable) {
            enableCustomService(account, paymentService, accountHasService);
        } else {
            if (accountHasService) {
                disableCustomService(account, paymentService);
                publisher.publishEvent(new UserDisabledServiceEvent(account.getId(), paymentService.getId()));
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
    
    private void checkSmsAllowness(PersonalAccount account) {
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
}