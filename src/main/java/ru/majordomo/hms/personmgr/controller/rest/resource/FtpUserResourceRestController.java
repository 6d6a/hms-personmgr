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
@RequestMapping("/{accountId}/ftp-user")
public class FtpUserResourceRestController extends CommonResourceRestController {
    private final static Logger logger = LoggerFactory.getLogger(FtpUserResourceRestController.class);

    @RequestMapping(value = "", method = RequestMethod.POST)
    public SimpleServiceMessage create(
            @RequestBody SimpleServiceMessage message,
            HttpServletResponse response,
            @PathVariable(value = "accountId", required = false) String accountId) {
        message.setAccountId(accountId);

        logger.debug(message.toString());

        if (accountId != null) {
            if (!planCheckerService.canAddFtpUser(accountId)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);

                return this.createErrorResponse("Plan limit for ftp-users exceeded");
            }
        }

        ProcessingBusinessAction businessAction = businessActionBuilder.build(BusinessActionType.FTP_USER_CREATE_RC, message);

        response.setStatus(HttpServletResponse.SC_ACCEPTED);

        return this.createSuccessResponse(businessAction);
    }

    @RequestMapping(value = "/{ftpuserId}", method = RequestMethod.PATCH)
    public SimpleServiceMessage update(
            @PathVariable String ftpuserId,
            @RequestBody SimpleServiceMessage message, HttpServletResponse response,
            @PathVariable(value = "accountId", required = false) String accountId) {
        message.setAccountId(accountId);

        logger.debug("Updating ftpuser with id " + ftpuserId + " " + message.toString());

        message.getParams().put("resourceId", ftpuserId);

        ProcessingBusinessAction businessAction = businessActionBuilder.build(BusinessActionType.FTP_USER_UPDATE_RC, message);

        response.setStatus(HttpServletResponse.SC_ACCEPTED);

        return this.createSuccessResponse(businessAction);
    }

    @RequestMapping(value = "/{ftpuserId}", method = RequestMethod.DELETE)
    public SimpleServiceMessage delete(
            @PathVariable String ftpuserId,
            HttpServletResponse response,
            @PathVariable(value = "accountId", required = false) String accountId) {
        SimpleServiceMessage message = new SimpleServiceMessage();
        message.setAccountId(accountId);
        message.addParam("resourceId", ftpuserId);
        message.setAccountId(accountId);

        logger.debug("Deleting ftpuser with id " + ftpuserId + " " + message.toString());

        ProcessingBusinessAction businessAction = businessActionBuilder.build(BusinessActionType.FTP_USER_DELETE_RC, message);

        response.setStatus(HttpServletResponse.SC_ACCEPTED);

        return this.createSuccessResponse(businessAction);
    }
}
