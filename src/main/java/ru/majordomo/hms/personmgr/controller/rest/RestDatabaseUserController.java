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

import ru.majordomo.hms.personmgr.common.BusinessActionType;
import ru.majordomo.hms.personmgr.common.message.ResponseMessage;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.model.ProcessingBusinessAction;
import ru.majordomo.hms.personmgr.repository.ProcessingBusinessActionRepository;
import ru.majordomo.hms.personmgr.service.BusinessActionBuilder;

/**
 * RestDatabaseUserController
 */
@RestController
@RequestMapping({"/{accountId}/database-user", "/database-user"})
public class RestDatabaseUserController extends CommonRestController {
    private final static Logger logger = LoggerFactory.getLogger(RestDatabaseUserController.class);

    @Autowired
    private BusinessActionBuilder businessActionBuilder;

    @Autowired
    private ProcessingBusinessActionRepository processingBusinessActionRepository;

    @RequestMapping(value = "", method = RequestMethod.POST)
    public SimpleServiceMessage create(
            @RequestBody SimpleServiceMessage message,
            HttpServletResponse response,
            @PathVariable(value = "accountId", required = false) String accountId) {
        message.setAccountId(accountId);

        logger.info(message.toString());

        ProcessingBusinessAction businessAction = businessActionBuilder.build(BusinessActionType.DATABASE_USER_CREATE_RC, message);

        response.setStatus(HttpServletResponse.SC_ACCEPTED);

        return this.createSuccessResponse(businessAction);
    }

    @RequestMapping(value = "/{databaseUserId}", method = RequestMethod.PATCH)
    public SimpleServiceMessage update(
            @PathVariable String databaseUserId,
            @RequestBody SimpleServiceMessage message, HttpServletResponse response,
            @PathVariable(value = "accountId", required = false) String accountId) {
        message.setAccountId(accountId);

        logger.info("Updating database user with id " + databaseUserId + " " + message.toString());

        message.getParams().put("id", databaseUserId);

        ProcessingBusinessAction businessAction = businessActionBuilder.build(BusinessActionType.DATABASE_USER_UPDATE_RC, message);

        response.setStatus(HttpServletResponse.SC_ACCEPTED);

        return this.createSuccessResponse(businessAction);
    }

    @RequestMapping(value = "/{databaseUserId}", method = RequestMethod.DELETE)
    public SimpleServiceMessage delete(
            @PathVariable String databaseUserId,
            HttpServletResponse response,
            @PathVariable(value = "accountId", required = false) String accountId) {
        SimpleServiceMessage message = new SimpleServiceMessage();
        message.setAccountId(accountId);
        message.addParam("databaseUserId", databaseUserId);
        message.setAccountId(accountId);

        logger.info("Deleting database user with id " + databaseUserId + " " + message.toString());

        ProcessingBusinessAction businessAction = businessActionBuilder.build(BusinessActionType.DATABASE_USER_DELETE_RC, message);

        response.setStatus(HttpServletResponse.SC_ACCEPTED);

        return this.createSuccessResponse(businessAction);
    }
}
