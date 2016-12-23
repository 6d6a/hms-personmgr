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
import ru.majordomo.hms.personmgr.common.message.*;
import ru.majordomo.hms.personmgr.model.ProcessingBusinessAction;

@RestController
@RequestMapping("/{accountId}/website")
public class RestWebSiteController extends CommonRestResourceController {
    private final static Logger logger = LoggerFactory.getLogger(RestWebSiteController.class);

    @RequestMapping(value = "", method = RequestMethod.POST)
    public SimpleServiceMessage create(
            @RequestBody SimpleServiceMessage message, HttpServletResponse response,
            @PathVariable(value = "accountId", required = false) String accountId) {
        message.setAccountId(accountId);

        logger.debug("Creating website: " + message.toString());

        if (accountId != null) {
            if (!planCheckerService.canAddWebSite(accountId)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);

                return this.createErrorResponse("Plan limit for websites exceeded");
            }
        }

        ProcessingBusinessAction businessAction = businessActionBuilder.build(BusinessActionType.WEB_SITE_CREATE_RC, message);

        response.setStatus(HttpServletResponse.SC_ACCEPTED);

        return this.createSuccessResponse(businessAction);
    }

    @RequestMapping(value = "/{websiteId}", method = RequestMethod.PATCH)
    public SimpleServiceMessage update(
            @PathVariable String websiteId,
            @RequestBody SimpleServiceMessage message, HttpServletResponse response,
            @PathVariable(value = "accountId", required = false) String accountId) {
        message.setAccountId(accountId);

        logger.debug("Updating website with id " + websiteId + " " + message.toString());

        message.getParams().put("resourceId", websiteId);

        ProcessingBusinessAction businessAction = businessActionBuilder.build(BusinessActionType.WEB_SITE_UPDATE_RC, message);

        response.setStatus(HttpServletResponse.SC_ACCEPTED);

        return this.createSuccessResponse(businessAction);
    }

    @RequestMapping(value = "/{websiteId}", method = RequestMethod.DELETE)
    public SimpleServiceMessage delete(
            @PathVariable String websiteId,
            HttpServletResponse response,
            @PathVariable(value = "accountId", required = false) String accountId) {
        SimpleServiceMessage message = new SimpleServiceMessage();
        message.setAccountId(accountId);
        message.addParam("resourceId", websiteId);
        message.setAccountId(accountId);

        logger.debug("Deleting website with id " + websiteId + " " + message.toString());

        ProcessingBusinessAction businessAction = businessActionBuilder.build(BusinessActionType.WEB_SITE_DELETE_RC, message);

        response.setStatus(HttpServletResponse.SC_ACCEPTED);

        return this.createSuccessResponse(businessAction);
    }
}
