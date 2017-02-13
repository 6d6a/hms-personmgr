package ru.majordomo.hms.personmgr.controller.rest.resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;

import ru.majordomo.hms.personmgr.common.BusinessActionType;
import ru.majordomo.hms.personmgr.common.BusinessOperationType;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.model.PersonalAccount;
import ru.majordomo.hms.personmgr.model.ProcessingBusinessAction;
import ru.majordomo.hms.personmgr.validators.ObjectId;

@RestController
@RequestMapping("/{accountId}/database-user")
@Validated
public class DatabaseUserResourceRestController extends CommonResourceRestController {
    private final static Logger logger = LoggerFactory.getLogger(DatabaseUserResourceRestController.class);

    @RequestMapping(value = "", method = RequestMethod.POST)
    public SimpleServiceMessage create(
            @RequestBody SimpleServiceMessage message,
            HttpServletResponse response,
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId
    ) {
        message.setAccountId(accountId);

        logger.debug("Creating database user " + message.toString());

        ProcessingBusinessAction businessAction = process(BusinessOperationType.DATABASE_USER_CREATE, BusinessActionType.DATABASE_USER_CREATE_RC, message);

        response.setStatus(HttpServletResponse.SC_ACCEPTED);

        return this.createSuccessResponse(businessAction);
    }

    @RequestMapping(value = "/{resourceId}", method = RequestMethod.PATCH)
    public SimpleServiceMessage update(
            @PathVariable String resourceId,
            @RequestBody SimpleServiceMessage message, HttpServletResponse response,
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId
    ) {
        message.setAccountId(accountId);
        message.getParams().put("resourceId", resourceId);

        logger.debug("Updating database user with id " + resourceId + " " + message.toString());

        ProcessingBusinessAction businessAction = process(BusinessOperationType.DATABASE_USER_UPDATE, BusinessActionType.DATABASE_USER_UPDATE_RC, message);

        response.setStatus(HttpServletResponse.SC_ACCEPTED);

        return this.createSuccessResponse(businessAction);
    }

    @RequestMapping(value = "/{resourceId}", method = RequestMethod.DELETE)
    public SimpleServiceMessage delete(
            @PathVariable String resourceId,
            HttpServletResponse response,
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId
    ) {
        SimpleServiceMessage message = new SimpleServiceMessage();
        message.addParam("resourceId", resourceId);
        message.setAccountId(accountId);

        logger.debug("Deleting database user with id " + resourceId + " " + message.toString());

        ProcessingBusinessAction businessAction = process(BusinessOperationType.DATABASE_USER_DELETE, BusinessActionType.DATABASE_USER_DELETE_RC, message);

        response.setStatus(HttpServletResponse.SC_ACCEPTED);

        return this.createSuccessResponse(businessAction);
    }
}
