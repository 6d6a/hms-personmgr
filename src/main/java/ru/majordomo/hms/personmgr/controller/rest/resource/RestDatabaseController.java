package ru.majordomo.hms.personmgr.controller.rest.resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;

import ru.majordomo.hms.personmgr.common.BusinessActionType;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.model.ProcessingBusinessAction;

@RestController
@RequestMapping("/{accountId}/database")
public class RestDatabaseController extends CommonRestResourceController {
    private final static Logger logger = LoggerFactory.getLogger(RestDatabaseController.class);

    @RequestMapping(value = "", method = RequestMethod.POST)
    public SimpleServiceMessage create(
            @RequestBody SimpleServiceMessage message,
            HttpServletResponse response,
            @PathVariable(value = "accountId", required = false) String accountId) {
        message.setAccountId(accountId);

        logger.info(message.toString());

        if (accountId != null) {
            if (!planCheckerService.canAddDatabase(accountId)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);

                return this.createErrorResponse("Plan limit for databases exceeded");
            }
        }

        ProcessingBusinessAction businessAction = businessActionBuilder.build(BusinessActionType.DATABASE_CREATE_RC, message);

        response.setStatus(HttpServletResponse.SC_ACCEPTED);

        return this.createSuccessResponse(businessAction);
    }

    @RequestMapping(value = "/{databaseId}", method = RequestMethod.PATCH)
    public SimpleServiceMessage update(
            @PathVariable String databaseId,
            @RequestBody SimpleServiceMessage message, HttpServletResponse response,
            @PathVariable(value = "accountId", required = false) String accountId) {
        message.setAccountId(accountId);

        logger.info("Updating database with id " + databaseId + " " + message.toString());

        message.addParam("resourceId", databaseId);

        ProcessingBusinessAction businessAction = businessActionBuilder.build(BusinessActionType.DATABASE_UPDATE_RC, message);

        response.setStatus(HttpServletResponse.SC_ACCEPTED);

        return this.createSuccessResponse(businessAction);
    }

    @RequestMapping(value = "/{databaseId}", method = RequestMethod.DELETE)
    public SimpleServiceMessage delete(
            @PathVariable String databaseId,
            HttpServletResponse response,
            @PathVariable(value = "accountId", required = false) String accountId) {
        SimpleServiceMessage message = new SimpleServiceMessage();
        message.setAccountId(accountId);
        message.addParam("resourceId", databaseId);
        message.setAccountId(accountId);

        logger.info("Deleting database with id " + databaseId + " " + message.toString());

        ProcessingBusinessAction businessAction = businessActionBuilder.build(BusinessActionType.DATABASE_DELETE_RC, message);

        response.setStatus(HttpServletResponse.SC_ACCEPTED);

        return this.createSuccessResponse(businessAction);
    }
}
