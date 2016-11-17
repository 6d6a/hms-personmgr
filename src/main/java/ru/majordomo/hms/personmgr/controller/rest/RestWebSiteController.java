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
import ru.majordomo.hms.personmgr.common.Count;
import ru.majordomo.hms.personmgr.common.message.*;
import ru.majordomo.hms.personmgr.model.ProcessingBusinessAction;
import ru.majordomo.hms.personmgr.repository.PlanRepository;
import ru.majordomo.hms.personmgr.repository.ProcessingBusinessActionRepository;
import ru.majordomo.hms.personmgr.service.BusinessActionBuilder;
import ru.majordomo.hms.personmgr.service.RcUserFeignClient;
import ru.majordomo.hms.personmgr.service.RcUserFeignClientFallback;

/**
 * WebSiteController
 */
@RestController
@RequestMapping({"/{accountId}/website", "/website"})
public class RestWebSiteController extends CommonRestController {
    private final static Logger logger = LoggerFactory.getLogger(RestWebSiteController.class);

    private final BusinessActionBuilder businessActionBuilder;

    private final RcUserFeignClient rcUserFeignClient;

    private final RcUserFeignClientFallback rcUserFeignClientFallback;

    @Autowired
    public RestWebSiteController(BusinessActionBuilder businessActionBuilder, RcUserFeignClient rcUserFeignClient, RcUserFeignClientFallback rcUserFeignClientFallback) {
        this.businessActionBuilder = businessActionBuilder;
        this.rcUserFeignClient = rcUserFeignClient;
        this.rcUserFeignClientFallback = rcUserFeignClientFallback;
    }

    @RequestMapping(value = "", method = RequestMethod.POST)
    public SimpleServiceMessage create(
            @RequestBody SimpleServiceMessage message, HttpServletResponse response,
            @PathVariable(value = "accountId", required = false) String accountId) {
        message.setAccountId(accountId);

        logger.info("Creating website: " + message.toString());

        if (accountId != null) {
            Count currentWebsiteCount = rcUserFeignClient.getWebsiteCount(accountId);
            Count planWebsiteCount = rcUserFeignClientFallback.getWebsiteCount(accountId);

            logger.info("Checking websites limit. currentWebsiteCount " + currentWebsiteCount + " planWebsiteCount " + planWebsiteCount);

            if (currentWebsiteCount.compareTo(planWebsiteCount) >= 0) {
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

        logger.info("Updating website with id " + websiteId + " " + message.toString());

        message.getParams().put("id", websiteId);

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
        message.addParam("websiteId", websiteId);
        message.setAccountId(accountId);

        logger.info("Deleting website with id " + websiteId + " " + message.toString());

        ProcessingBusinessAction businessAction = businessActionBuilder.build(BusinessActionType.WEB_SITE_DELETE_RC, message);

        response.setStatus(HttpServletResponse.SC_ACCEPTED);

        return this.createSuccessResponse(businessAction);
    }
}
