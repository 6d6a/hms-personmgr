package ru.majordomo.hms.personmgr.controller.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;

import javax.servlet.http.HttpServletResponse;

import ru.majordomo.hms.personmgr.common.FlowType;
import ru.majordomo.hms.personmgr.common.RestResponse;
import ru.majordomo.hms.personmgr.common.message.ResponseMessage;
import ru.majordomo.hms.personmgr.common.message.ResponseMessageParams;
import ru.majordomo.hms.personmgr.common.message.WebSiteCreateMessage;
import ru.majordomo.hms.personmgr.common.message.rest.RestMessage;
import ru.majordomo.hms.personmgr.model.ProcessingBusinessFlow;
import ru.majordomo.hms.personmgr.repository.ProcessingBusinessFlowRepository;
import ru.majordomo.hms.personmgr.service.BusinessFlowBuilder;

/**
 * WebSiteController
 */
@RestController
@RequestMapping("/website")
@CrossOrigin("*")
public class RestWebSiteController {
    private final static Logger logger = LoggerFactory.getLogger(RestWebSiteController.class);

    @Autowired
    private BusinessFlowBuilder businessFlowBuilder;

    @Autowired
    private ProcessingBusinessFlowRepository processingBusinessFlowRepository;

    @RequestMapping(value = "create", method = RequestMethod.POST)
    public ResponseMessage create(
            @RequestBody WebSiteCreateMessage message, HttpServletResponse response
    ) {
        logger.info(message.toString());
        //handling request - getting body params
//        RestMessage restMessage = RestHelper.getFromJson(requestBody);

//        HashMap<Object, Object> data = restMessage.getParams();


        ProcessingBusinessFlow processingBusinessFlow = businessFlowBuilder.build(FlowType.WEB_SITE_CREATE, message.getParams());

        processingBusinessFlowRepository.save(processingBusinessFlow);

        response.setStatus(HttpServletResponse.SC_ACCEPTED);

        ResponseMessage responseMessage = new ResponseMessage();
        ResponseMessageParams messageParams = new ResponseMessageParams();
        messageParams.setSuccess(true);
        responseMessage.setParams(messageParams);
        responseMessage.setOperationIdentity(processingBusinessFlow.getId());
//        responseMessage.setActionIdentity("11");

        return responseMessage;
//        return new RestResponse(processingBusinessFlow.getId(), processingBusinessFlow.toString());
    }
}
