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
import ru.majordomo.hms.personmgr.event.accountHistory.AccountHistoryEvent;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.manager.AccountOwnerManager;
import ru.majordomo.hms.personmgr.model.account.AccountOwner;
import ru.majordomo.hms.personmgr.model.account.ContactInfo;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.account.PersonalInfo;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessAction;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessOperation;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.model.service.AccountService;
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
    private final PromocodeProcessor promocodeProcessor;
    private final AccountServiceRepository accountServiceRepository;
    private final AccountOwnerManager accountOwnerManager;
    private final PlanLimitsService planLimitsService;
    private final SiFeignClient siFeignClient;
    private final RcUserFeignClient rcUserFeignClient;
    private final AccountTransferService accountTransferService;

    @Autowired
    public AccountResourceRestController(
            SequenceCounterService sequenceCounterService,
            ProcessingBusinessOperationRepository processingBusinessOperationRepository,
            PlanRepository planRepository,
            PromocodeProcessor promocodeProcessor,
            AccountServiceRepository accountServiceRepository,
            AccountOwnerManager accountOwnerManager,
            PlanLimitsService planLimitsService,
            SiFeignClient siFeignClient,
            RcUserFeignClient rcUserFeignClient,
            AccountTransferService accountTransferService
    ) {
        this.sequenceCounterService = sequenceCounterService;
        this.processingBusinessOperationRepository = processingBusinessOperationRepository;
        this.planRepository = planRepository;
        this.promocodeProcessor = promocodeProcessor;
        this.accountServiceRepository = accountServiceRepository;
        this.accountOwnerManager = accountOwnerManager;
        this.planLimitsService = planLimitsService;
        this.siFeignClient = siFeignClient;
        this.rcUserFeignClient = rcUserFeignClient;
        this.accountTransferService = accountTransferService;
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

        //Create pm, si and fin account
        String accountId = String.valueOf(sequenceCounterService.getNextSequence("PersonalAccount"));
        String password = randomAlphabetic(8);

        Plan plan = planRepository.findByOldId((String) message.getParam("plan"));

        if (plan == null) {
            logger.debug("No plan found with OldId: " + message.getParam("plan"));
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

        //Создаем PersonalAccount
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

        //Сохраняем данные о владельце аккаунта
        ContactInfo contactInfo = new ContactInfo();
        contactInfo.setEmailAddresses(emails);

        AccountOwner accountOwner = new AccountOwner();
        accountOwner.setPersonalAccountId(personalAccount.getId());
        accountOwner.setContactInfo(contactInfo);
        accountOwner.setName((String) message.getParam("name"));
        accountOwner.setType(accountOwnerType);

        String inn = (String) message.getParam("inn");
        if (inn != null) {
            PersonalInfo personalInfo = new PersonalInfo();
            personalInfo.setInn(inn);

            accountOwner.setPersonalInfo(personalInfo);
        }

        accountOwnerManager.insert(accountOwner);
        logger.debug("accountOwner saved: " + accountOwner.toString());

        //Создаем AccountService с выбранным тарифом
        AccountService service = new AccountService();
        service.setPersonalAccountId(personalAccount.getId());
        service.setServiceId(plan.getServiceId());

        accountServiceRepository.save(service);
        logger.debug("AccountService saved: " + service.toString());

        //Сохраним в мессагу квоту по тарифу
        Long planQuotaKBFreeLimit = planLimitsService.getQuotaKBFreeLimit(plan);
        message.addParam("quota", planQuotaKBFreeLimit);

        //генерируем партнерский промокод
        promocodeProcessor.generatePartnerPromocode(personalAccount);
        logger.debug("PartnerPromocode generated");

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
                String operator = request.getUserPrincipal().getName();
                String historyMessage = "Сервер аккаунта " + accountId + " изменён на " + desiredServerId;
                Map<String, String> historyParams = new HashMap<>();

                historyParams.put(HISTORY_MESSAGE_KEY, historyMessage);
                historyParams.put(OPERATOR_KEY, operator);

                publisher.publishEvent(new AccountHistoryEvent(accountId, historyParams));
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

        //Save history
        String operator = request.getUserPrincipal() != null ? request.getUserPrincipal().getName() : "service";
        Map<String, String> params = new HashMap<>();
        params.put(HISTORY_MESSAGE_KEY, "Поступила заявка на перенос акканута на serverId: " + desiredServerId);
        params.put(OPERATOR_KEY, operator);

        publisher.publishEvent(new AccountHistoryEvent(accountId, params));

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
}
