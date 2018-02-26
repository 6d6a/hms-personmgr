package ru.majordomo.hms.personmgr.controller.rest.resource;

import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestWrapper;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;

import ru.majordomo.hms.personmgr.common.BusinessActionType;
import ru.majordomo.hms.personmgr.common.BusinessOperationType;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.controller.rest.CommonRestController;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessAction;
import ru.majordomo.hms.personmgr.validation.ObjectId;

@RestController
@RequestMapping("/{accountId}/ftp-user")
@Validated
public class FtpUserResourceRestController extends CommonRestController {
    @RequestMapping(value = "", method = RequestMethod.POST)
    public SimpleServiceMessage create(
            @RequestBody SimpleServiceMessage message,
            HttpServletResponse response,
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            SecurityContextHolderAwareRequestWrapper request
    ) {
        message.setAccountId(accountId);

        logger.debug("Creating ftpuser " + message.toString());

        if (!accountManager.findOne(accountId).isActive()) {
            throw new ParameterValidationException("Аккаунт неактивен. Создание FTP пользователя невозможно.");
        }

        if (!planCheckerService.canAddFtpUser(accountId)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);

            return this.createErrorResponse("Plan limit for ftp-users exceeded");
        }

        ProcessingBusinessAction businessAction = businessHelper.buildActionAndOperation(BusinessOperationType.FTP_USER_CREATE, BusinessActionType.FTP_USER_CREATE_RC, message);

        response.setStatus(HttpServletResponse.SC_ACCEPTED);

        saveHistory(request, accountId, "Поступила заявка на создание FTP-пользователя (имя: " + message.getParam("name") + ")");

        return this.createSuccessResponse(businessAction);
    }

    @RequestMapping(value = "/{resourceId}", method = RequestMethod.PATCH)
    public SimpleServiceMessage update(
            @PathVariable String resourceId,
            @RequestBody SimpleServiceMessage message,
            HttpServletResponse response,
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            SecurityContextHolderAwareRequestWrapper request
    ) {
        message.setAccountId(accountId);
        message.getParams().put("resourceId", resourceId);

        logger.debug("Updating ftpuser with id " + resourceId + " " + message.toString());

        if (!accountManager.findOne(accountId).isActive()) {
            throw new ParameterValidationException("Аккаунт неактивен. Обновление FTP пользователя невозможно.");
        }

        ProcessingBusinessAction businessAction = businessHelper.buildActionAndOperation(BusinessOperationType.FTP_USER_UPDATE, BusinessActionType.FTP_USER_UPDATE_RC, message);

        response.setStatus(HttpServletResponse.SC_ACCEPTED);

        saveHistory(request, accountId, "Поступила заявка на обновление FTP-пользователя (Id: " + resourceId  + ", имя: " + message.getParam("name") + ")");

        return this.createSuccessResponse(businessAction);
    }

    @RequestMapping(value = "/{resourceId}", method = RequestMethod.DELETE)
    public SimpleServiceMessage delete(
            @PathVariable String resourceId,
            HttpServletResponse response,
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            SecurityContextHolderAwareRequestWrapper request
    ) {
        SimpleServiceMessage message = new SimpleServiceMessage();
        message.addParam("resourceId", resourceId);
        message.setAccountId(accountId);

        logger.debug("Deleting ftpuser with id " + resourceId + " " + message.toString());

        ProcessingBusinessAction businessAction = businessHelper.buildActionAndOperation(BusinessOperationType.FTP_USER_DELETE, BusinessActionType.FTP_USER_DELETE_RC, message);

        response.setStatus(HttpServletResponse.SC_ACCEPTED);

        saveHistory(request, accountId, "Поступила заявка на удаление FTP-пользователя (Id: " + resourceId  + ", имя: " + message.getParam("name") + ")");

        return this.createSuccessResponse(businessAction);
    }
}
