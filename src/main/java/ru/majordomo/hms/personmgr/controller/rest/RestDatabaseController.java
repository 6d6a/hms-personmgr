package ru.majordomo.hms.personmgr.controller.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;

import ru.majordomo.hms.personmgr.common.FlowType;
import ru.majordomo.hms.personmgr.common.message.DatabaseCreateMessage;
import ru.majordomo.hms.personmgr.common.message.ResponseMessage;
import ru.majordomo.hms.personmgr.common.message.ResponseMessageParams;
import ru.majordomo.hms.personmgr.model.ProcessingBusinessFlow;
import ru.majordomo.hms.personmgr.repository.ProcessingBusinessFlowRepository;
import ru.majordomo.hms.personmgr.service.BusinessFlowBuilder;

/**
 * WebSiteController
 */
@RestController
@RequestMapping("/database")
@CrossOrigin("*")
public class RestDatabaseController {
    private final static Logger logger = LoggerFactory.getLogger(RestDatabaseController.class);

    @Autowired
    private BusinessFlowBuilder businessFlowBuilder;

    @Autowired
    private ProcessingBusinessFlowRepository processingBusinessFlowRepository;

    @RequestMapping(value = "create", method = RequestMethod.POST)
    public ResponseMessage create(
            @RequestBody DatabaseCreateMessage message,
            HttpServletResponse response
    ) {
        logger.info(message.toString());

        ProcessingBusinessFlow processingBusinessFlow = businessFlowBuilder.build(FlowType.DATABASE_CREATE, message);

        processingBusinessFlowRepository.save(processingBusinessFlow);

        response.setStatus(HttpServletResponse.SC_ACCEPTED);

        ResponseMessage responseMessage = new ResponseMessage();
        ResponseMessageParams messageParams = new ResponseMessageParams();
        messageParams.setSuccess(true);
        responseMessage.setParams(messageParams);
        responseMessage.setOperationIdentity(processingBusinessFlow.getId());

        return responseMessage;
    }
}
