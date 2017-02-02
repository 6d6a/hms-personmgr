package ru.majordomo.hms.personmgr.controller.rest.resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;

import ru.majordomo.hms.personmgr.common.BusinessActionType;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.model.ProcessingBusinessAction;

@RestController
@RequestMapping("/{accountId}/mailbox")
public class MailboxResourceRestController extends CommonResourceRestController {
    private final static Logger logger = LoggerFactory.getLogger(MailboxResourceRestController.class);

    @RequestMapping(value = "", method = RequestMethod.POST)
    public SimpleServiceMessage create(
            @RequestBody SimpleServiceMessage message, HttpServletResponse response,
            @PathVariable(value = "accountId", required = false) String accountId) {
        message.setAccountId(accountId);

        logger.debug("Creating mailbox: " + message.toString());

        ProcessingBusinessAction businessAction = businessActionBuilder.build(BusinessActionType.MAILBOX_CREATE_RC, message);

        response.setStatus(HttpServletResponse.SC_ACCEPTED);

        return this.createSuccessResponse(businessAction);
    }

    @RequestMapping(value = "/{mailboxId}", method = RequestMethod.PATCH)
    public SimpleServiceMessage update(
            @PathVariable String mailboxId,
            @RequestBody SimpleServiceMessage message, HttpServletResponse response,
            @PathVariable(value = "accountId", required = false) String accountId) {
        message.setAccountId(accountId);

        logger.debug("Updating mailbox with id " + mailboxId + " " + message.toString());

        message.getParams().put("resourceId", mailboxId);

        ProcessingBusinessAction businessAction = businessActionBuilder.build(BusinessActionType.MAILBOX_UPDATE_RC, message);

        response.setStatus(HttpServletResponse.SC_ACCEPTED);

        return this.createSuccessResponse(businessAction);
    }

    @RequestMapping(value = "/{mailboxId}", method = RequestMethod.DELETE)
    public SimpleServiceMessage delete(
            @PathVariable String mailboxId,
            HttpServletResponse response,
            @PathVariable(value = "accountId", required = false) String accountId) {
        SimpleServiceMessage message = new SimpleServiceMessage();
        message.setAccountId(accountId);
        message.addParam("resourceId", mailboxId);
        message.setAccountId(accountId);

        logger.debug("Deleting mailbox with id " + mailboxId + " " + message.toString());

        ProcessingBusinessAction businessAction = businessActionBuilder.build(BusinessActionType.MAILBOX_DELETE_RC, message);

        response.setStatus(HttpServletResponse.SC_ACCEPTED);

        return this.createSuccessResponse(businessAction);
    }
}
