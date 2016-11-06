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
 * RestDomainController
 */
@RestController
@RequestMapping({"/{accountId}/domain", "/domain"})
public class RestDomainController extends CommonRestController {
    private final static Logger logger = LoggerFactory.getLogger(RestDomainController.class);

    @Autowired
    private BusinessActionBuilder businessActionBuilder;

    @Autowired
    private ProcessingBusinessActionRepository processingBusinessActionRepository;

    @RequestMapping(value = "", method = RequestMethod.POST)
    public ResponseMessage create(
            @RequestBody SimpleServiceMessage message,
            HttpServletResponse response,
            @PathVariable(value = "accountId", required = false) String accountId) {
        message.setAccountId(accountId);

        logger.info(message.toString());

        ProcessingBusinessAction businessAction = businessActionBuilder.build(BusinessActionType.DOMAIN_CREATE_RC, message);

        processingBusinessActionRepository.save(businessAction);

        response.setStatus(HttpServletResponse.SC_ACCEPTED);

        return this.createResponse(businessAction);
    }

    @RequestMapping(value = "/{domainId}", method = RequestMethod.PATCH)
    public ResponseMessage update(
            @PathVariable String domainId,
            @RequestBody SimpleServiceMessage message, HttpServletResponse response,
            @PathVariable(value = "accountId", required = false) String accountId) {
        message.setAccountId(accountId);

        logger.info("Updating domain with id " + domainId + " " + message.toString());

        message.getParams().put("id", domainId);

        ProcessingBusinessAction businessAction = businessActionBuilder.build(BusinessActionType.DOMAIN_UPDATE_RC, message);

        processingBusinessActionRepository.save(businessAction);

        response.setStatus(HttpServletResponse.SC_ACCEPTED);

        return this.createResponse(businessAction);
    }

    @RequestMapping(value = "/{domainId}", method = RequestMethod.DELETE)
    public ResponseMessage delete(
            @PathVariable String domainId,
            @RequestBody SimpleServiceMessage message, HttpServletResponse response,
            @PathVariable(value = "accountId", required = false) String accountId) {
        message.setAccountId(accountId);

        logger.info("Deleting domain with id " + domainId + " " + message.toString());

        message.getParams().put("id", domainId);

        ProcessingBusinessAction businessAction = businessActionBuilder.build(BusinessActionType.DOMAIN_DELETE_RC, message);

        processingBusinessActionRepository.save(businessAction);

        response.setStatus(HttpServletResponse.SC_ACCEPTED);

        return this.createResponse(businessAction);
    }
}
