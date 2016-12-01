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
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.model.ProcessingBusinessAction;
import ru.majordomo.hms.personmgr.service.BusinessActionBuilder;
import ru.majordomo.hms.personmgr.service.PlanCheckerService;

/**
 * RestFtpUserController
 */
@RestController
@RequestMapping({"/{accountId}/ftp-user", "/ftp-user"})
public class RestFtpUserController extends CommonRestController {
    private final static Logger logger = LoggerFactory.getLogger(RestFtpUserController.class);

    private final BusinessActionBuilder businessActionBuilder;

    private final PlanCheckerService planCheckerService;

    @Autowired
    public RestFtpUserController(
            BusinessActionBuilder businessActionBuilder,
            PlanCheckerService planCheckerService) {
        this.businessActionBuilder = businessActionBuilder;
        this.planCheckerService = planCheckerService;
    }

    @RequestMapping(value = "", method = RequestMethod.POST)
    public SimpleServiceMessage create(
            @RequestBody SimpleServiceMessage message,
            HttpServletResponse response,
            @PathVariable(value = "accountId", required = false) String accountId) {
        message.setAccountId(accountId);

        logger.info(message.toString());

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

        logger.info("Updating ftpuser with id " + ftpuserId + " " + message.toString());

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

        logger.info("Deleting ftpuser with id " + ftpuserId + " " + message.toString());

        ProcessingBusinessAction businessAction = businessActionBuilder.build(BusinessActionType.FTP_USER_DELETE_RC, message);

        response.setStatus(HttpServletResponse.SC_ACCEPTED);

        return this.createSuccessResponse(businessAction);
    }
}
