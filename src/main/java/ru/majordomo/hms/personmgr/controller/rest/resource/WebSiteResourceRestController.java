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
import ru.majordomo.hms.personmgr.common.message.*;
import ru.majordomo.hms.personmgr.model.PersonalAccount;
import ru.majordomo.hms.personmgr.model.ProcessingBusinessAction;
import ru.majordomo.hms.personmgr.validators.ObjectId;

@RestController
@RequestMapping("/{accountId}/website")
@Validated
public class WebSiteResourceRestController extends CommonResourceRestController {
    private final static Logger logger = LoggerFactory.getLogger(WebSiteResourceRestController.class);

    @RequestMapping(value = "", method = RequestMethod.POST)
    public SimpleServiceMessage create(
            @RequestBody SimpleServiceMessage message,
            HttpServletResponse response,
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId
    ) {
        message.setAccountId(accountId);

        logger.debug("Creating website: " + message.toString());

        if (!planCheckerService.canAddWebSite(accountId)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);

            return this.createErrorResponse("Plan limit for websites exceeded");
        }

        ProcessingBusinessAction businessAction = process(BusinessOperationType.WEB_SITE_CREATE, BusinessActionType.WEB_SITE_CREATE_RC, message);

        response.setStatus(HttpServletResponse.SC_ACCEPTED);

        return this.createSuccessResponse(businessAction);
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

        logger.debug("Updating website with id " + resourceId + " " + message.toString());

        ProcessingBusinessAction businessAction = process(BusinessOperationType.WEB_SITE_UPDATE, BusinessActionType.WEB_SITE_UPDATE_RC, message);

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

        logger.debug("Deleting website with id " + resourceId + " " + message.toString());

        ProcessingBusinessAction businessAction = process(BusinessOperationType.WEB_SITE_DELETE, BusinessActionType.WEB_SITE_DELETE_RC, message);

        response.setStatus(HttpServletResponse.SC_ACCEPTED);

        return this.createSuccessResponse(businessAction);
    }
}
