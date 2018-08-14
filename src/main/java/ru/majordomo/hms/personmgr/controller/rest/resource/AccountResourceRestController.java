package ru.majordomo.hms.personmgr.controller.rest.resource;

import org.apache.commons.validator.routines.EmailValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestWrapper;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import ru.majordomo.hms.personmgr.common.AccountType;
import ru.majordomo.hms.personmgr.common.BusinessActionType;
import ru.majordomo.hms.personmgr.common.BusinessOperationType;
import ru.majordomo.hms.personmgr.common.MailManagerMessageType;
import ru.majordomo.hms.personmgr.common.Utils;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.controller.rest.CommonRestController;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.manager.AccountOwnerManager;
import ru.majordomo.hms.personmgr.model.account.AccountOwner;
import ru.majordomo.hms.personmgr.model.account.ContactInfo;
import ru.majordomo.hms.personmgr.model.account.InfoBannerAccountNotice;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.account.PersonalInfo;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessAction;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessOperation;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.model.service.AccountService;
import ru.majordomo.hms.personmgr.repository.AccountNoticeRepository;
import ru.majordomo.hms.personmgr.repository.AccountServiceRepository;
import ru.majordomo.hms.personmgr.repository.PlanRepository;
import ru.majordomo.hms.personmgr.repository.ProcessingBusinessOperationRepository;
import ru.majordomo.hms.personmgr.service.*;
import ru.majordomo.hms.personmgr.validation.ObjectId;

import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static ru.majordomo.hms.personmgr.common.Constants.*;
import static ru.majordomo.hms.personmgr.common.RequiredField.ACCOUNT_CREATE;
import static ru.majordomo.hms.personmgr.common.RequiredField.ACCOUNT_TRANSFER;

@RestController
public class AccountResourceRestController extends CommonRestController {
    private final SequenceCounterService sequenceCounterService;
    private final ProcessingBusinessOperationRepository processingBusinessOperationRepository;
    private final PlanRepository planRepository;
    private final AccountServiceRepository accountServiceRepository;
    private final AccountOwnerManager accountOwnerManager;
    private final PlanLimitsService planLimitsService;
    private final SiFeignClient siFeignClient;
    private final RcUserFeignClient rcUserFeignClient;
    private final AccountTransferService accountTransferService;
    private final AccountNoticeRepository accountNoticeRepository;

    @Autowired
    public AccountResourceRestController(
            SequenceCounterService sequenceCounterService,
            ProcessingBusinessOperationRepository processingBusinessOperationRepository,
            PlanRepository planRepository,
            AccountServiceRepository accountServiceRepository,
            AccountOwnerManager accountOwnerManager,
            PlanLimitsService planLimitsService,
            SiFeignClient siFeignClient,
            RcUserFeignClient rcUserFeignClient,
            AccountTransferService accountTransferService,
            AccountNoticeRepository accountNoticeRepository
    ) {
        this.sequenceCounterService = sequenceCounterService;
        this.processingBusinessOperationRepository = processingBusinessOperationRepository;
        this.planRepository = planRepository;
        this.accountServiceRepository = accountServiceRepository;
        this.accountOwnerManager = accountOwnerManager;
        this.planLimitsService = planLimitsService;
        this.siFeignClient = siFeignClient;
        this.rcUserFeignClient = rcUserFeignClient;
        this.accountTransferService = accountTransferService;
        this.accountNoticeRepository = accountNoticeRepository;
    }


    @SuppressWarnings("unchecked")
    @RequestMapping(value = "/register", method = RequestMethod.POST)
    public SimpleServiceMessage create(
            @RequestBody SimpleServiceMessage message,
            HttpServletResponse response
    ) {
        logger.debug("Got SimpleServiceMessage: " + message.toString());

        Utils.checkRequiredParams(message.getParams(), ACCOUNT_CREATE);

        boolean agreement = (boolean) message.getParam("agreement");

        if (!agreement) {
            logger.debug("Agreement not accepted");
            return this.createErrorResponse("Необходимо согласиться с условиями оферты");
        }

        String password = randomAlphabetic(8);

        Plan plan = planRepository.findByOldId((String) message.getParam("plan"));

        if (plan == null) {
            logger.debug("No plan found with OldId: " + message.getParam("plan"));
            return this.createErrorResponse("Не найден тарифный план с id: " + message.getParam("plan"));
        }

        if (!plan.isActive()) {
            logger.info("Попытка регистрации на неактивном тарифе. message: " + message.toString());
            return this.createErrorResponse("Не найден тарифный план с id: " + message.getParam("plan"));
        }

        EmailValidator validator = EmailValidator.getInstance(true, true);
        List<String> emails = (List<String>) message.getParam("emailAddresses");

        for (String emailAddress: emails) {
            if (!validator.isValid(emailAddress)) {
                return this.createErrorResponse("Адрес " + emailAddress + " некорректен");
            }
        }

        AccountOwner.Type accountOwnerType;
        try {
            accountOwnerType = AccountOwner.Type.valueOf((String) message.getParam("type"));
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            logger.debug("No or wrong type of accountOwner: " + message.getParam("type"));
            return this.createErrorResponse("Не указан, либо неверно указан тип владельца аккаунта");
        }

        PersonalAccount personalAccount = createPersonalAccount(plan);

        createAccountOwner(personalAccount, emails, accountOwnerType, message);

        createAccountService(personalAccount, plan);

        createInfoBannerAccountNotice(personalAccount);

        //Сохраним в мессагу квоту по тарифу
        Long planQuotaKBFreeLimit = planLimitsService.getQuotaKBFreeLimit(plan);
        message.addParam("quota", planQuotaKBFreeLimit);
        message.setAccountId(personalAccount.getId());
        message.addParam("username", personalAccount.getName());
        message.addParam(PASSWORD_KEY, password);

        ProcessingBusinessOperation processingBusinessOperation = businessHelper.buildOperation(BusinessOperationType.ACCOUNT_CREATE, message);

        logger.debug("processingBusinessOperation saved: " + processingBusinessOperation.toString());

        SimpleServiceMessage siResponse = siFeignClient.createWebAccessAccount(message);

        if (siResponse.getParam("success") == null || !((boolean) siResponse.getParam("success"))) {
            return this.createErrorResponse("WebAccessAccount creation failed. Response params:" + siResponse.getParams());
        }

        message.addParam("token", siResponse.getParam("token"));

        ProcessingBusinessAction businessAction = businessHelper.buildActionByOperation(BusinessActionType.ACCOUNT_CREATE_FIN, message, processingBusinessOperation);

        logger.debug("ProcessingBusinessAction saved: " + businessAction.toString());

        processingBusinessOperation.addParam("token", siResponse.getParam("token"));
        processingBusinessOperationRepository.save(processingBusinessOperation);

        response.setStatus(HttpServletResponse.SC_ACCEPTED);

        SimpleServiceMessage responseMessage = this.createSuccessResponse(businessAction);
        responseMessage.addParam("token", siResponse.getParam("token"));

        Map<String, String> credentials = new HashMap<>();
        credentials.put("username", personalAccount.getName());
        credentials.put(PASSWORD_KEY, password);

        responseMessage.addParam("credentials", credentials);

        return responseMessage;
    }

    @PreAuthorize("hasAuthority('TRANSFER_ACCOUNT')")
    @RequestMapping(value = "/account/{accountId}/move", method = RequestMethod.POST)
    public Boolean moveAccount(
            @ObjectId(PersonalAccount.class) @PathVariable("accountId") String accountId,
            @RequestBody Map<String, String> params,
            SecurityContextHolderAwareRequestWrapper request,
            Authentication authentication
    ) {
        Utils.checkRequiredParams(params, ACCOUNT_TRANSFER);

        String desiredServerId = params.get(SERVER_ID_KEY);
        if (desiredServerId.equals("")) {
            return false;
        }

        try {
            Map<String, String> body = new HashMap<>();
            body.put(SERVER_ID_KEY, desiredServerId);
            Boolean success = rcUserFeignClient.moveAccount(accountId, body);

            if (success) {
                history.save(accountId, "Сервер аккаунта " + accountId + " изменён на " + desiredServerId, request);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    @PreAuthorize("hasAuthority('TRANSFER_ACCOUNT')")
    @PostMapping(value = "/account/{accountId}/transfer")
    public SimpleServiceMessage transferAccount(
            @ObjectId(PersonalAccount.class) @PathVariable("accountId") String accountId,
            @RequestBody SimpleServiceMessage message,
            SecurityContextHolderAwareRequestWrapper request,
            HttpServletResponse response,
            Authentication authentication
    ) {
        message.setAccountId(accountId);
        Utils.checkRequiredParams(message.getParams(), ACCOUNT_TRANSFER);

        String desiredServerId = (String) message.getParam(SERVER_ID_KEY);

        if (desiredServerId.equals("")) {
            throw new ParameterValidationException("Передано пустое значение в параметре " + SERVER_ID_KEY);
        }

        ProcessingBusinessAction businessAction;

        try {
            businessAction = accountTransferService.startTransfer(message);
        } catch (Exception e) {
            if (!(e instanceof ParameterValidationException)) {
                e.printStackTrace();
            }
            return this.createErrorResponse(e.getMessage());
        }

        response.setStatus(HttpServletResponse.SC_ACCEPTED);

        history.save(accountId, "Поступила заявка на перенос акканута на serverId: " + desiredServerId, request);

        return this.createSuccessResponse(businessAction);
    }
//
//    @RequestMapping(value = "/{accountId}", method = RequestMethod.PATCH)
//    public SimpleServiceMessage update(
//            @PathVariable String accountId,
//            @RequestBody SimpleServiceMessage message, HttpServletResponse response
//    ) {
//        logger.debug("Updating account with id " + accountId + " " + message.toString());
//
//        message.addParam("accountId", accountId);
//
//        ProcessingBusinessAction businessAction = businessActionBuilder.build(BusinessActionType.ACCOUNT_UPDATE_RC, message);
//
//        response.setStatus(HttpServletResponse.SC_ACCEPTED);
//
//        return this.createSuccessResponse(businessAction);
//    }
//
//    @RequestMapping(value = "/{accountId}", method = RequestMethod.DELETE)
//    public SimpleServiceMessage delete(
//            @PathVariable String accountId,
//            HttpServletResponse response
//    ) {
//        SimpleServiceMessage message = new SimpleServiceMessage();
//        message.setAccountId(accountId);
//        message.addParam("accountId", accountId);
//        message.setAccountId(accountId);
//
//        logger.debug("Deleting account with id " + accountId + " " + message.toString());
//
//        ProcessingBusinessAction businessAction = businessActionBuilder.build(BusinessActionType.ACCOUNT_DELETE_RC, message);
//
//        response.setStatus(HttpServletResponse.SC_ACCEPTED);
//
//        return this.createSuccessResponse(businessAction);
//    }

    private PersonalAccount createPersonalAccount(Plan plan){
        String accountId = String.valueOf(sequenceCounterService.getNextSequence("PersonalAccount"));

        PersonalAccount personalAccount = new PersonalAccount();
        personalAccount.setAccountType(AccountType.VIRTUAL_HOSTING);
        personalAccount.setPlanId(plan.getId());
        personalAccount.setAccountId(accountId);
        personalAccount.setId(accountId);
        personalAccount.setClientId(accountId);
        personalAccount.setName(VH_ACCOUNT_PREFIX + accountId);
        personalAccount.setActive(!plan.isAbonementOnly());
        personalAccount.setCreated(LocalDateTime.now());
        personalAccount.setAccountNew(true);
        personalAccount.setCredit(false);
        personalAccount.setCreditPeriod("P14D");

        //Установка уведомлений по-умолчанию (почтовая информационная рассылка)
        Set<MailManagerMessageType> defaultNotifications = new HashSet<>();
        defaultNotifications.add(MailManagerMessageType.EMAIL_NEWS);
        personalAccount.setNotifications(defaultNotifications);

        accountManager.insert(personalAccount);
        logger.debug("personalAccount saved: " + personalAccount.toString());

        return personalAccount;
    }

    private void createAccountOwner(
            PersonalAccount personalAccount,
            List<String> emails,
            AccountOwner.Type accountOwnerType,
            SimpleServiceMessage message
    ) {
        String name = (String) message.getParam("name");
        String inn = (String) message.getParam("inn");

        ContactInfo contactInfo = new ContactInfo();
        contactInfo.setEmailAddresses(emails);

        AccountOwner accountOwner = new AccountOwner();
        accountOwner.setPersonalAccountId(personalAccount.getId());
        accountOwner.setContactInfo(contactInfo);
        accountOwner.setName(name);
        accountOwner.setType(accountOwnerType);

        if (inn != null) {
            PersonalInfo personalInfo = new PersonalInfo();
            personalInfo.setInn(inn);

            accountOwner.setPersonalInfo(personalInfo);
        }

        accountOwnerManager.insert(accountOwner);
        logger.debug("accountOwner saved: " + accountOwner.toString());
    }

    private void createAccountService(PersonalAccount personalAccount, Plan plan) {
        AccountService service = new AccountService();
        service.setPersonalAccountId(personalAccount.getId());
        service.setServiceId(plan.getServiceId());

        accountServiceRepository.save(service);
        logger.debug("AccountService saved: " + service.toString());
    }

    private void createInfoBannerAccountNotice(PersonalAccount personalAccount){
        InfoBannerAccountNotice notification = new InfoBannerAccountNotice();
        notification.setPersonalAccountId(personalAccount.getId());
        notification.setCreated(LocalDateTime.now());
        notification.setViewed(false);
        notification.setComponent("hello_user");

        accountNoticeRepository.save(notification);
        logger.debug("InfoBannerAccountNotice saved: " + notification.toString());
    }
}
