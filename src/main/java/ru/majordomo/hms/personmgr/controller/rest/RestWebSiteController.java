package ru.majordomo.hms.personmgr.controller.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletResponse;

import ru.majordomo.hms.personmgr.common.BusinessActionType;
import ru.majordomo.hms.personmgr.common.message.*;
import ru.majordomo.hms.personmgr.model.ProcessingBusinessAction;
import ru.majordomo.hms.personmgr.repository.ProcessingBusinessActionRepository;
import ru.majordomo.hms.personmgr.service.BusinessActionBuilder;

/**
 * WebSiteController
 */
@RequestMapping("/website")
public class RestWebSiteController extends CommonRestController {
    private final static Logger logger = LoggerFactory.getLogger(RestWebSiteController.class);

    @Autowired
    private BusinessActionBuilder businessActionBuilder;

    @Autowired
    private ProcessingBusinessActionRepository processingBusinessActionRepository;

    @RequestMapping(value = "", method = RequestMethod.POST)
    public ResponseMessage create(
            @RequestBody SimpleServiceMessage message, HttpServletResponse response
    ) {
        logger.info("Creating website: " + message.toString());

        ProcessingBusinessAction businessAction = businessActionBuilder.build(BusinessActionType.WEB_SITE_CREATE_RC, message);

        processingBusinessActionRepository.save(businessAction);

        response.setStatus(HttpServletResponse.SC_ACCEPTED);

        return this.createResponse(businessAction);
    }

    @RequestMapping(value = "/{websiteId}", method = RequestMethod.PATCH)
    public ResponseMessage update(
            @PathVariable String websiteId,
            @RequestBody SimpleServiceMessage message, HttpServletResponse response
    ) {
        logger.info("Updating website with id " + websiteId + " " + message.toString());

        message.getParams().put("id", websiteId);

        ProcessingBusinessAction businessAction = businessActionBuilder.build(BusinessActionType.WEB_SITE_UPDATE_RC, message);

        processingBusinessActionRepository.save(businessAction);

        response.setStatus(HttpServletResponse.SC_ACCEPTED);

        return this.createResponse(businessAction);
    }

    @RequestMapping(value = "/{websiteId}", method = RequestMethod.DELETE)
    public ResponseMessage delete(
            @PathVariable String websiteId,
            @RequestBody SimpleServiceMessage message, HttpServletResponse response
    ) {
        logger.info("Deleting website with id " + websiteId + " " + message.toString());

        message.getParams().put("id", websiteId);

        ProcessingBusinessAction businessAction = businessActionBuilder.build(BusinessActionType.WEB_SITE_DELETE_RC, message);

        processingBusinessActionRepository.save(businessAction);

        response.setStatus(HttpServletResponse.SC_ACCEPTED);

        return this.createResponse(businessAction);
    }
}
