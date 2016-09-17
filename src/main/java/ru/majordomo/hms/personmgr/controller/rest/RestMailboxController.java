package ru.majordomo.hms.personmgr.controller.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletResponse;

import ru.majordomo.hms.personmgr.common.ActionType;
import ru.majordomo.hms.personmgr.common.message.ResponseMessage;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.model.ProcessingBusinessAction;
import ru.majordomo.hms.personmgr.repository.ProcessingBusinessActionRepository;
import ru.majordomo.hms.personmgr.service.BusinessActionBuilder;

/**
 * WebSiteController
 */
@RequestMapping("/website")
public class RestMailboxController extends CommonRestController {
    private final static Logger logger = LoggerFactory.getLogger(RestMailboxController.class);

    @Autowired
    private BusinessActionBuilder businessActionBuilder;

    @Autowired
    private ProcessingBusinessActionRepository processingBusinessActionRepository;

    @RequestMapping(value = "", method = RequestMethod.POST)
    public ResponseMessage create(
            @RequestBody SimpleServiceMessage message, HttpServletResponse response
    ) {
        logger.info("Creating mailbox: " + message.toString());

        ProcessingBusinessAction businessAction = businessActionBuilder.build(ActionType.MAILBOX_CREATE_RC, message);

        processingBusinessActionRepository.save(businessAction);

        response.setStatus(HttpServletResponse.SC_ACCEPTED);

        return this.createResponse(businessAction);
    }

    @RequestMapping(value = "/{mailboxId}", method = RequestMethod.PATCH)
    public ResponseMessage update(
            @PathVariable String mailboxId,
            @RequestBody SimpleServiceMessage message, HttpServletResponse response
    ) {
        logger.info("Updating mailbox with id " + mailboxId + " " + message.toString());

        message.getParams().put("id", mailboxId);

        ProcessingBusinessAction businessAction = businessActionBuilder.build(ActionType.MAILBOX_UPDATE_RC, message);

        processingBusinessActionRepository.save(businessAction);

        response.setStatus(HttpServletResponse.SC_ACCEPTED);

        return this.createResponse(businessAction);
    }
}