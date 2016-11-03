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
import ru.majordomo.hms.personmgr.common.message.ResponseMessage;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.model.ProcessingBusinessAction;
import ru.majordomo.hms.personmgr.repository.ProcessingBusinessActionRepository;
import ru.majordomo.hms.personmgr.service.BusinessActionBuilder;

/**
 * RestUnixAccountController
 */
@RestController
@RequestMapping("/unix-account")
public class RestUnixAccountController extends CommonRestController {
    private final static Logger logger = LoggerFactory.getLogger(RestUnixAccountController.class);

    @Autowired
    private BusinessActionBuilder businessActionBuilder;

    @Autowired
    private ProcessingBusinessActionRepository processingBusinessActionRepository;

    @RequestMapping(value = "", method = RequestMethod.POST)
    public ResponseMessage create(
            @RequestBody SimpleServiceMessage message,
            HttpServletResponse response
    ) {
        logger.info(message.toString());

        ProcessingBusinessAction businessAction = businessActionBuilder.build(BusinessActionType.UNIX_ACCOUNT_CREATE_RC, message);

        processingBusinessActionRepository.save(businessAction);

        response.setStatus(HttpServletResponse.SC_ACCEPTED);

        return this.createResponse(businessAction);
    }

    @RequestMapping(value = "/{unixaccountId}", method = RequestMethod.PATCH)
    public ResponseMessage update(
            @PathVariable String unixaccountId,
            @RequestBody SimpleServiceMessage message, HttpServletResponse response
    ) {
        logger.info("Updating unix account with id " + unixaccountId + " " + message.toString());

        message.getParams().put("id", unixaccountId);

        ProcessingBusinessAction businessAction = businessActionBuilder.build(BusinessActionType.UNIX_ACCOUNT_UPDATE_RC, message);

        processingBusinessActionRepository.save(businessAction);

        response.setStatus(HttpServletResponse.SC_ACCEPTED);

        return this.createResponse(businessAction);
    }

    @RequestMapping(value = "/{unixaccountId}", method = RequestMethod.DELETE)
    public ResponseMessage delete(
            @PathVariable String unixaccountId,
            @RequestBody SimpleServiceMessage message, HttpServletResponse response
    ) {
        logger.info("Deleting unix account with id " + unixaccountId + " " + message.toString());

        message.getParams().put("id", unixaccountId);

        ProcessingBusinessAction businessAction = businessActionBuilder.build(BusinessActionType.UNIX_ACCOUNT_DELETE_RC, message);

        processingBusinessActionRepository.save(businessAction);

        response.setStatus(HttpServletResponse.SC_ACCEPTED);

        return this.createResponse(businessAction);
    }
}
