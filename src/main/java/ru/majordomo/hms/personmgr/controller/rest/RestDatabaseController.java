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

import ru.majordomo.hms.personmgr.common.ActionType;
import ru.majordomo.hms.personmgr.common.message.DatabaseCreateMessage;
import ru.majordomo.hms.personmgr.common.message.ResponseMessage;
import ru.majordomo.hms.personmgr.common.message.ResponseMessageParams;
import ru.majordomo.hms.personmgr.model.ProcessingBusinessAction;
import ru.majordomo.hms.personmgr.repository.ProcessingBusinessActionRepository;
import ru.majordomo.hms.personmgr.service.BusinessActionBuilder;

/**
 * WebSiteController
 */
@RestController
@RequestMapping("/database")
@CrossOrigin("*")
public class RestDatabaseController {
    private final static Logger logger = LoggerFactory.getLogger(RestDatabaseController.class);

    @Autowired
    private BusinessActionBuilder businessActionBuilder;

    @Autowired
    private ProcessingBusinessActionRepository processingBusinessActionRepository;

    @RequestMapping(value = "create", method = RequestMethod.POST)
    public ResponseMessage create(
            @RequestBody DatabaseCreateMessage message,
            HttpServletResponse response
    ) {
        logger.info(message.toString());

        ProcessingBusinessAction processingBusinessAction = businessActionBuilder.build(ActionType.DATABASE_CREATE, message);

        processingBusinessActionRepository.save(processingBusinessAction);

        response.setStatus(HttpServletResponse.SC_ACCEPTED);

        ResponseMessage responseMessage = new ResponseMessage();
        ResponseMessageParams messageParams = new ResponseMessageParams();
        messageParams.setSuccess(true);
        responseMessage.setParams(messageParams);
        responseMessage.setOperationIdentity(processingBusinessAction.getId());

        return responseMessage;
    }
}
