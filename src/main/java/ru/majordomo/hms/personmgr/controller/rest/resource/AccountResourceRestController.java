package ru.majordomo.hms.personmgr.controller.rest.resource;

import org.apache.commons.validator.routines.EmailValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import ru.majordomo.hms.personmgr.common.AccountType;
import ru.majordomo.hms.personmgr.common.BusinessActionType;
import ru.majordomo.hms.personmgr.common.BusinessOperationType;
import ru.majordomo.hms.personmgr.common.State;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.model.PersonalAccount;
import ru.majordomo.hms.personmgr.model.ProcessingBusinessAction;
import ru.majordomo.hms.personmgr.model.ProcessingBusinessOperation;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.model.service.AccountService;
import ru.majordomo.hms.personmgr.repository.AccountServiceRepository;
import ru.majordomo.hms.personmgr.repository.PersonalAccountRepository;
import ru.majordomo.hms.personmgr.repository.PlanRepository;
import ru.majordomo.hms.personmgr.repository.ProcessingBusinessOperationRepository;
import ru.majordomo.hms.personmgr.service.PlanLimitsService;
import ru.majordomo.hms.personmgr.service.PromocodeProcessor;
import ru.majordomo.hms.personmgr.service.SequenceCounterService;
import ru.majordomo.hms.personmgr.service.SiFeignClient;

import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static ru.majordomo.hms.personmgr.common.Constants.VH_ACCOUNT_PREFIX;
import static ru.majordomo.hms.personmgr.common.RequiredField.ACCOUNT_CREATE;

@RestController
@RequestMapping("/account")
public class AccountResourceRestController extends CommonResourceRestController {
    private final static Logger logger = LoggerFactory.getLogger(AccountResourceRestController.class);

    private final SequenceCounterService sequenceCounterService;
    private final PersonalAccountRepository personalAccountRepository;
    private final ProcessingBusinessOperationRepository processingBusinessOperationRepository;
    private final PlanRepository planRepository;
    private final PromocodeProcessor promocodeProcessor;
    private final AccountServiceRepository accountServiceRepository;
    private final PlanLimitsService planLimitsService;
    private final SiFeignClient siFeignClient;

    @Autowired
    public AccountResourceRestController(
            SequenceCounterService sequenceCounterService,
            PersonalAccountRepository personalAccountRepository,
            ProcessingBusinessOperationRepository processingBusinessOperationRepository,
            PlanRepository planRepository,
            PromocodeProcessor promocodeProcessor,
            AccountServiceRepository accountServiceRepository,
            PlanLimitsService planLimitsService,
            SiFeignClient siFeignClient) {
        this.sequenceCounterService = sequenceCounterService;
        this.personalAccountRepository = personalAccountRepository;
        this.processingBusinessOperationRepository = processingBusinessOperationRepository;
        this.planRepository = planRepository;
        this.promocodeProcessor = promocodeProcessor;
        this.accountServiceRepository = accountServiceRepository;
        this.planLimitsService = planLimitsService;
        this.siFeignClient = siFeignClient;
    }


    @SuppressWarnings("unchecked")
    @RequestMapping(value = "", method = RequestMethod.POST)
    public SimpleServiceMessage create(
            @RequestBody SimpleServiceMessage message,
            HttpServletResponse response
    ) {
        logger.debug("Got SimpleServiceMessage: " + message.toString());

        for (String field : ACCOUNT_CREATE) {
            if (message.getParam(field) == null) {
                logger.debug("No " + field + " property found in request");
                return this.createErrorResponse("No " + field + " property found in request");
            }
        }

        boolean agreement = (boolean) message.getParam("agreement");

        if (!agreement) {
            logger.debug("Agreement not accepted");
            return this.createErrorResponse("Agreement not accepted");
        }

        //Create pm, si and fin account
        String accountId = String.valueOf(sequenceCounterService.getNextSequence("PersonalAccount"));
        String password = randomAlphabetic(8);

        Plan plan = planRepository.findByOldId((String) message.getParam("plan"));

        if (plan == null) {
            logger.debug("No plan found with OldId: " + message.getParam("plan"));
            return this.createErrorResponse("No plan found with OldId: " + message.getParam("plan"));
        }

        EmailValidator validator = EmailValidator.getInstance(true, true);
        List<String> emails = (List<String>) message.getParam("emailAddresses");

        for (String emailAddress: emails) {
            if (!validator.isValid(emailAddress)) {
                return this.createErrorResponse("Адрес " + emailAddress + " некорректен");
            }
        }

        //Создаем PersonalAccount
        PersonalAccount personalAccount = new PersonalAccount();
        personalAccount.setAccountType(AccountType.VIRTUAL_HOSTING);
        personalAccount.setPlanId(plan.getId());
        personalAccount.setAccountId(accountId);
        personalAccount.setClientId(accountId);
        personalAccount.setName(VH_ACCOUNT_PREFIX + accountId);
        personalAccount.setActive(true);
        personalAccount.setCreated(LocalDateTime.now());

        personalAccountRepository.save(personalAccount);
        logger.debug("personalAccount saved: " + personalAccount.toString());

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

        ProcessingBusinessOperation processingBusinessOperation = new ProcessingBusinessOperation();
        processingBusinessOperation.setPersonalAccountId(personalAccount.getId());
        processingBusinessOperation.setState(State.PROCESSING);
        processingBusinessOperation.setAccountName(personalAccount.getName());
        processingBusinessOperation.setMapParams(message.getParams());
        processingBusinessOperation.addMapParam("password", password);
        processingBusinessOperation.setType(BusinessOperationType.ACCOUNT_CREATE);

        processingBusinessOperationRepository.save(processingBusinessOperation);

        logger.debug("processingBusinessOperation saved: " + processingBusinessOperation.toString());

        message.setAccountId(personalAccount.getId());
        message.setOperationIdentity(processingBusinessOperation.getId());
        message.addParam("username", personalAccount.getName());

        SimpleServiceMessage siResponse = siFeignClient.createWebAccessAccount(message);

        if (siResponse.getParam("success") == null || !((boolean) siResponse.getParam("success"))) {
            return this.createErrorResponse("WebAccessAccount creation failed. Response params:" + siResponse.getParams());
        }

        message.addParam("token", siResponse.getParam("token"));

        ProcessingBusinessAction businessAction = businessActionBuilder.build(BusinessActionType.ACCOUNT_CREATE_FIN, message);

        logger.debug("ProcessingBusinessAction saved: " + businessAction.toString());

        processingBusinessOperation.addMapParam("token", siResponse.getParam("token"));
        processingBusinessOperationRepository.save(processingBusinessOperation);

        response.setStatus(HttpServletResponse.SC_ACCEPTED);

        SimpleServiceMessage responseMessage = this.createSuccessResponse(businessAction);
        responseMessage.addParam("token", siResponse.getParam("token"));

        return responseMessage;
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
