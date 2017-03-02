package ru.majordomo.hms.personmgr.controller.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.model.PersonalAccount;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.model.service.AccountService;
import ru.majordomo.hms.personmgr.model.service.PaymentService;
import ru.majordomo.hms.personmgr.repository.AccountServiceRepository;
import ru.majordomo.hms.personmgr.repository.PaymentServiceRepository;
import ru.majordomo.hms.personmgr.repository.PersonalAccountRepository;
import ru.majordomo.hms.personmgr.repository.PlanRepository;
import ru.majordomo.hms.personmgr.service.AccountHelper;
import ru.majordomo.hms.personmgr.service.AccountServiceHelper;
import ru.majordomo.hms.personmgr.validators.ObjectId;

import static ru.majordomo.hms.personmgr.common.Constants.ANTI_SPAM_SERVICE_ID;
import static ru.majordomo.hms.personmgr.common.Constants.ENABLED_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.SMS_NOTIFICATIONS_29_RUB_SERVICE_ID;
import static ru.majordomo.hms.personmgr.common.RequiredField.ACCOUNT_SERVICE_CREATE;
import static ru.majordomo.hms.personmgr.common.RequiredField.ACCOUNT_SERVICE_ENABLE;


@RestController
@Validated
public class AccountServiceRestController extends CommonRestController {

    private final AccountServiceRepository accountServiceRepository;
    private final PaymentServiceRepository serviceRepository;
    private final PersonalAccountRepository accountRepository;
    private final PlanRepository planRepository;
    private final AccountServiceHelper accountServiceHelper;
    private final AccountHelper accountHelper;

    @Autowired
    public AccountServiceRestController(
            AccountServiceRepository accountServiceRepository,
            PaymentServiceRepository serviceRepository,
            PersonalAccountRepository accountRepository,
            PlanRepository planRepository,
            AccountServiceHelper accountServiceHelper,
            AccountHelper accountHelper
    ) {
        this.accountServiceRepository = accountServiceRepository;
        this.serviceRepository = serviceRepository;
        this.accountRepository = accountRepository;
        this.planRepository = planRepository;
        this.accountServiceHelper = accountServiceHelper;
        this.accountHelper = accountHelper;
    }

    @RequestMapping(value = "/{accountId}/account-service/{accountServiceId}",
                    method = RequestMethod.GET)
    public ResponseEntity<AccountService> get(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @ObjectId(AccountService.class) @PathVariable(value = "accountServiceId") String accountServiceId
    ) {
        PersonalAccount account = accountRepository.findByAccountId(accountId);

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
        PersonalAccount account = accountRepository.findByAccountId(accountId);

        Page<AccountService> accountServices = accountServiceRepository.findByPersonalAccountId(account.getId(), pageable);

        return new ResponseEntity<>(accountServices, HttpStatus.OK);
    }

    @RequestMapping(value = "/{accountId}/account-service",
                    method = RequestMethod.POST)
    public ResponseEntity<SimpleServiceMessage> addService(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @RequestBody Map<String, Object> requestBody
    ) {
        PersonalAccount account = accountRepository.findOne(accountId);

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

        return new ResponseEntity<>(this.createSuccessResponse("accountService created with id " + paymentServiceId), HttpStatus.OK);
    }

    @RequestMapping(value = "/{accountId}/account-service-sms-notification",
                    method = RequestMethod.GET)
    public ResponseEntity<AccountService> getSmsService(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId
    ) {
        PersonalAccount account = accountRepository.findByAccountId(accountId);

        PaymentService paymentService = getSmsPaymentServiceByPlanId(account.getPlanId());

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
            @RequestBody Map<String, Object> requestBody
    ) {
        PersonalAccount account = accountRepository.findOne(accountId);

        checkRequiredParams(requestBody, ACCOUNT_SERVICE_ENABLE);

        Boolean enabled = (Boolean) requestBody.get(ENABLED_KEY);

        PaymentService paymentService = getSmsPaymentServiceByPlanId(account.getPlanId());

        processCustomService(account, paymentService, enabled);

        return new ResponseEntity<>(this.createSuccessResponse("accountService " + (enabled ? "enabled" : "disabled") + " for sms-notification"), HttpStatus.OK);
    }

    @RequestMapping(value = "/{accountId}/account-service-anti-spam",
                    method = RequestMethod.GET)
    public ResponseEntity<AccountService> getAntiSpamService(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId
    ) {
        PersonalAccount account = accountRepository.findByAccountId(accountId);

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
            @RequestBody Map<String, Object> requestBody
    ) {
        PersonalAccount account = accountRepository.findOne(accountId);

        checkRequiredParams(requestBody, ACCOUNT_SERVICE_ENABLE);

        Boolean enabled = (Boolean) requestBody.get(ENABLED_KEY);

        PaymentService paymentService = getPaymentServiceByOldId(ANTI_SPAM_SERVICE_ID);

        processCustomService(account, paymentService, enabled);

        return new ResponseEntity<>(this.createSuccessResponse("accountService " + (enabled ? "enabled" : "disabled") + " for anti-spam"), HttpStatus.OK);
    }

    @RequestMapping(value = "/{accountId}/account-service/{accountServiceId}",
                    method = RequestMethod.DELETE)
    public ResponseEntity<Object> delete(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @ObjectId(AccountService.class) @PathVariable(value = "accountServiceId") String accountServiceId
    ) {
        PersonalAccount account = accountRepository.findOne(accountId);

        accountServiceHelper.deleteAccountServiceById(account, accountServiceId);

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

    private PaymentService getSmsPaymentServiceByPlanId(String planId) {
        Plan plan = planRepository.findOne(planId);

        if (plan == null) {
            throw new ParameterValidationException("Plan with id " + planId + " not found");
        }

        String smsServiceId = plan.getSmsServiceId();

        PaymentService paymentService;

        if (smsServiceId == null) {
            smsServiceId = SMS_NOTIFICATIONS_29_RUB_SERVICE_ID;
            paymentService = serviceRepository.findByOldId(smsServiceId);
        } else {
            paymentService = serviceRepository.findOne(smsServiceId);
        }

        if (paymentService == null) {
            throw new ParameterValidationException("paymentService with id " + smsServiceId + " not found");
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

                if (currentService.getLastBilled() == null || currentService.getLastBilled().compareTo(LocalDateTime.now().minusDays(1)) <= 0) {
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