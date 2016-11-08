package ru.majordomo.hms.personmgr.controller.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;

import ru.majordomo.hms.personmgr.common.AccountType;
import ru.majordomo.hms.personmgr.common.BusinessActionType;
import ru.majordomo.hms.personmgr.common.State;
import ru.majordomo.hms.personmgr.common.message.ResponseMessage;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.model.PersonalAccount;
import ru.majordomo.hms.personmgr.model.ProcessingBusinessAction;
import ru.majordomo.hms.personmgr.model.ProcessingBusinessOperation;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.repository.PersonalAccountRepository;
import ru.majordomo.hms.personmgr.repository.PlanRepository;
import ru.majordomo.hms.personmgr.repository.ProcessingBusinessActionRepository;
import ru.majordomo.hms.personmgr.repository.ProcessingBusinessOperationRepository;
import ru.majordomo.hms.personmgr.service.BusinessActionBuilder;
import ru.majordomo.hms.personmgr.service.SequenceCounterService;

import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static ru.majordomo.hms.personmgr.common.StringConstants.PLAN_SERVICE_PREFIX;
import static ru.majordomo.hms.personmgr.common.StringConstants.VH_ACCOUNT_PREFIX;

/**
 * RestAccountController
 */
@RestController
@RequestMapping("/account")
public class RestAccountController extends CommonRestController {
    private final static Logger logger = LoggerFactory.getLogger(RestAccountController.class);

    @Autowired
    private SequenceCounterService sequenceCounterService;

    @Autowired
    private BusinessActionBuilder businessActionBuilder;

    @Autowired
    private ProcessingBusinessActionRepository processingBusinessActionRepository;

    @Autowired
    private PersonalAccountRepository personalAccountRepository;

    @Autowired
    private ProcessingBusinessOperationRepository processingBusinessOperationRepository;

    @Autowired
    private PlanRepository planRepository;

    @RequestMapping(value = "", method = RequestMethod.POST)
    public SimpleServiceMessage create(
            @RequestBody SimpleServiceMessage message,
            HttpServletResponse response
    ) {
        logger.info(message.toString());
        //Create pm, si and fin account

        String accountId = String.valueOf(sequenceCounterService.getNextSequence("PersonalAccount"));
        String password = randomAlphabetic(8);

        Plan plan = planRepository.findByOldId((String) message.getParam("plan"));

        if (plan == null) {
            return this.createErrorResponse("No plan found with OldId: " + message.getParam("plan"));
        }

        message.addParam("planServiceId", plan.getFinServiceId());

        PersonalAccount personalAccount = new PersonalAccount();
        personalAccount.setAccountType(AccountType.VIRTUAL_HOSTING);
        personalAccount.setPlanId(plan.getId());
        personalAccount.setAccountId(accountId);
        personalAccount.setClientId(accountId);
        personalAccount.setName(VH_ACCOUNT_PREFIX + accountId);

        personalAccountRepository.save(personalAccount);


        ProcessingBusinessOperation processingBusinessOperation = new ProcessingBusinessOperation();
        processingBusinessOperation.setPersonalAccountId(personalAccount.getId());
        processingBusinessOperation.setState(State.PROCESSING);
        processingBusinessOperation.setAccountName(personalAccount.getName());
        processingBusinessOperation.setMapParams(message.getParams());
        processingBusinessOperation.addMapParam("password", password);

        processingBusinessOperationRepository.save(processingBusinessOperation);

        message.setAccountId(personalAccount.getId());
        message.setOperationIdentity(processingBusinessOperation.getId());
        message.addParam("username", personalAccount.getName());

        ProcessingBusinessAction businessAction = businessActionBuilder.build(BusinessActionType.ACCOUNT_CREATE_SI, message);

        processingBusinessActionRepository.save(businessAction);

        response.setStatus(HttpServletResponse.SC_ACCEPTED);

        return this.createSuccessResponse(businessAction);
    }

    @RequestMapping(value = "/{accountId}", method = RequestMethod.PATCH)
    public SimpleServiceMessage update(
            @PathVariable String accountId,
            @RequestBody SimpleServiceMessage message, HttpServletResponse response
    ) {
        logger.info("Updating account with id " + accountId + " " + message.toString());

        message.getParams().put("id", accountId);

        ProcessingBusinessAction businessAction = businessActionBuilder.build(BusinessActionType.ACCOUNT_UPDATE_RC, message);

        processingBusinessActionRepository.save(businessAction);

        response.setStatus(HttpServletResponse.SC_ACCEPTED);

        return this.createSuccessResponse(businessAction);
    }

    @RequestMapping(value = "/{accountId}", method = RequestMethod.DELETE)
    public SimpleServiceMessage delete(
            @PathVariable String accountId,
            @RequestBody SimpleServiceMessage message, HttpServletResponse response
    ) {
        logger.info("Deleting account with id " + accountId + " " + message.toString());

        message.getParams().put("id", accountId);

        ProcessingBusinessAction businessAction = businessActionBuilder.build(BusinessActionType.ACCOUNT_DELETE_RC, message);

        processingBusinessActionRepository.save(businessAction);

        response.setStatus(HttpServletResponse.SC_ACCEPTED);

        return this.createSuccessResponse(businessAction);
    }
}
