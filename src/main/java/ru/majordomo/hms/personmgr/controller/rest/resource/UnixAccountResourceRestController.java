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
@RequestMapping("/{accountId}/unix-account")
public class UnixAccountResourceRestController extends CommonResourceRestController {
    private final static Logger logger = LoggerFactory.getLogger(UnixAccountResourceRestController.class);

    @RequestMapping(value = "", method = RequestMethod.POST)
    public SimpleServiceMessage create(
            @RequestBody SimpleServiceMessage message,
            HttpServletResponse response,
            @PathVariable(value = "accountId", required = false) String accountId) {
        message.setAccountId(accountId);

        logger.debug(message.toString());

        ProcessingBusinessAction businessAction = businessActionBuilder.build(BusinessActionType.UNIX_ACCOUNT_CREATE_RC, message);

        response.setStatus(HttpServletResponse.SC_ACCEPTED);

        return this.createSuccessResponse(businessAction);
    }

    @RequestMapping(value = "/{unixaccountId}", method = RequestMethod.PATCH)
    public SimpleServiceMessage update(
            @PathVariable String unixaccountId,
            @RequestBody SimpleServiceMessage message, HttpServletResponse response,
            @PathVariable(value = "accountId", required = false) String accountId) {
        message.setAccountId(accountId);

        logger.debug("Updating unix account with id " + unixaccountId + " " + message.toString());

        message.getParams().put("resourceId", unixaccountId);

        ProcessingBusinessAction businessAction = businessActionBuilder.build(BusinessActionType.UNIX_ACCOUNT_UPDATE_RC, message);

        response.setStatus(HttpServletResponse.SC_ACCEPTED);

        return this.createSuccessResponse(businessAction);
    }

    @RequestMapping(value = "/{unixaccountId}", method = RequestMethod.DELETE)
    public SimpleServiceMessage delete(
            @PathVariable String unixaccountId,
            HttpServletResponse response,
            @PathVariable(value = "accountId", required = false) String accountId) {
        SimpleServiceMessage message = new SimpleServiceMessage();
        message.setAccountId(accountId);
        message.addParam("resourceId", unixaccountId);
        message.setAccountId(accountId);

        logger.debug("Deleting unix account with id " + unixaccountId + " " + message.toString());

        ProcessingBusinessAction businessAction = businessActionBuilder.build(BusinessActionType.UNIX_ACCOUNT_DELETE_RC, message);

        response.setStatus(HttpServletResponse.SC_ACCEPTED);

        return this.createSuccessResponse(businessAction);
    }
}
