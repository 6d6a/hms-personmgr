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
@RequestMapping("/{accountId}/database")
@Validated
public class DatabaseResourceRestController extends CommonResourceRestController {
    private final static Logger logger = LoggerFactory.getLogger(DatabaseResourceRestController.class);

    @RequestMapping(value = "", method = RequestMethod.POST)
    public SimpleServiceMessage create(
            @RequestBody SimpleServiceMessage message,
            HttpServletResponse response,
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId
    ) {
        message.setAccountId(accountId);

        logger.debug("Creating database " + message.toString());

        if (!planCheckerService.canAddDatabase(accountId)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);

            return this.createErrorResponse("Plan limit for databases exceeded");
        }

        ProcessingBusinessAction businessAction = process(BusinessOperationType.DATABASE_CREATE, BusinessActionType.DATABASE_CREATE_RC, message);

        response.setStatus(HttpServletResponse.SC_ACCEPTED);

        return createSuccessResponse(businessAction);
    }

    @RequestMapping(value = "/{resourceId}", method = RequestMethod.PATCH)
    public SimpleServiceMessage update(
            @PathVariable String resourceId,
            @RequestBody SimpleServiceMessage message,
            HttpServletResponse response,
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId
    ) {
        message.setAccountId(accountId);
        message.addParam("resourceId", resourceId);

        logger.debug("Updating database with id " + resourceId + " " + message.toString());

        ProcessingBusinessAction businessAction = process(BusinessOperationType.DATABASE_UPDATE, BusinessActionType.DATABASE_UPDATE_RC, message);

        response.setStatus(HttpServletResponse.SC_ACCEPTED);

        return createSuccessResponse(businessAction);
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

        logger.debug("Deleting database with id " + resourceId + " " + message.toString());

        ProcessingBusinessAction businessAction = process(BusinessOperationType.DATABASE_DELETE, BusinessActionType.DATABASE_DELETE_RC, message);

        response.setStatus(HttpServletResponse.SC_ACCEPTED);

        return createSuccessResponse(businessAction);
    }
}
