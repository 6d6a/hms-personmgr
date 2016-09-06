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
import ru.majordomo.hms.personmgr.common.message.rest.RestMessage;
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
    public RestResponse create(
            @RequestBody String requestBody,
            HttpServletResponse response
    ) {
        logger.info(requestBody);
        //handling request - getting body params
        RestMessage restMessage = RestHelper.getFromJson(requestBody);

        String operationIdentity = restMessage.getOperationIdentity();

        HashMap<Object, Object> data = restMessage.getParams();

        if (!RestHelper.isValidOperationIdentity(operationIdentity)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            logger.info("OperationIdentity is not valid");
            return new RestResponse("0", "Bad operationIdentity");
        }

        ProcessingBusinessFlow processingBusinessFlow = businessFlowBuilder.build(FlowType.DATABASE_CREATE, data);

        processingBusinessFlowRepository.save(processingBusinessFlow);

        response.setStatus(HttpServletResponse.SC_ACCEPTED);

        return new RestResponse(processingBusinessFlow.getId(), processingBusinessFlow.toString());
    }
}
