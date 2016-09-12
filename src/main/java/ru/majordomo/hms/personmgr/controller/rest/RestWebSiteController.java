package ru.majordomo.hms.personmgr.controller.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;

import ru.majordomo.hms.personmgr.common.ActionType;
import ru.majordomo.hms.personmgr.common.message.ResponseMessage;
import ru.majordomo.hms.personmgr.common.message.ResponseMessageParams;
import ru.majordomo.hms.personmgr.common.message.WebSiteCreateMessage;
import ru.majordomo.hms.personmgr.common.message.WebSiteCreateMessageParams;
import ru.majordomo.hms.personmgr.model.ProcessingBusinessFlow;
import ru.majordomo.hms.personmgr.repository.ProcessingBusinessActionRepository;
import ru.majordomo.hms.personmgr.service.BusinessActionBuilder;

/**
 * WebSiteController
 */
@RestController
@RequestMapping("/website")
@CrossOrigin("*")
public class RestWebSiteController {
    private final static Logger logger = LoggerFactory.getLogger(RestWebSiteController.class);

    @Autowired
    private BusinessActionBuilder businessActionBuilder;

    @Autowired
    private ProcessingBusinessActionRepository processingBusinessActionRepository;

    @RequestMapping(value = "", method = RequestMethod.POST)
    public ResponseMessage create(
            @RequestBody WebSiteCreateMessage message, HttpServletResponse response
    ) {
        logger.info(message.toString());

        ProcessingBusinessFlow processingBusinessFlow = businessActionBuilder.build(ActionType.WEB_SITE_CREATE_RC, message);

        processingBusinessActionRepository.save(processingBusinessFlow);

        response.setStatus(HttpServletResponse.SC_ACCEPTED);

        ResponseMessage responseMessage = new ResponseMessage();
        ResponseMessageParams messageParams = new ResponseMessageParams();
        messageParams.setSuccess(true);
        responseMessage.setParams(messageParams);
        responseMessage.setOperationIdentity(processingBusinessFlow.getId());

        return responseMessage;
    }

    @RequestMapping(value = "/{websiteId}", method = RequestMethod.PATCH)
    public ResponseMessage update(
            @PathVariable String websiteId,
            @RequestBody WebSiteCreateMessage message, HttpServletResponse response
    ) {
        logger.info("Updating website with id " + websiteId + " " + message.toString());

        WebSiteCreateMessageParams params = message.getParams();
        params.setId(websiteId);

        message.setParams(params);
        ProcessingBusinessFlow processingBusinessFlow = businessActionBuilder.build(ActionType.WEB_SITE_UPDATE, message);

        processingBusinessActionRepository.save(processingBusinessFlow);

        response.setStatus(HttpServletResponse.SC_ACCEPTED);

        ResponseMessage responseMessage = new ResponseMessage();
        ResponseMessageParams messageParams = new ResponseMessageParams();
        messageParams.setSuccess(true);
        responseMessage.setParams(messageParams);
        responseMessage.setOperationIdentity(processingBusinessFlow.getId());

        return responseMessage;
    }
}
