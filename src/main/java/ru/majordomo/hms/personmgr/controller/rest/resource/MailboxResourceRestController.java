package ru.majordomo.hms.personmgr.controller.rest.resource;

import org.springframework.security.core.Authentication;
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
import ru.majordomo.hms.personmgr.common.ResourceType;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.controller.rest.CommonRestController;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessAction;
import ru.majordomo.hms.personmgr.validation.ObjectId;

import static ru.majordomo.hms.personmgr.common.FieldRoles.MAILBOX_PATCH;
import static ru.majordomo.hms.personmgr.common.FieldRoles.MAILBOX_POST;

@RestController
@RequestMapping("/{accountId}/mailbox")
@Validated
public class MailboxResourceRestController extends CommonRestController {
    @RequestMapping(value = "", method = RequestMethod.POST)
    public SimpleServiceMessage create(
            @RequestBody SimpleServiceMessage message,
            HttpServletResponse response,
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            SecurityContextHolderAwareRequestWrapper request,
            Authentication authentication
    ) {
        message.setAccountId(accountId);

        logger.debug("Creating mailbox: " + message.toString());

        PersonalAccount account = accountManager.findOne(accountId);

        if (!account.isActive()) {
            throw new ParameterValidationException("Аккаунт неактивен. Создание почтового ящика невозможно.");
        }

        if (request.isUserInRole("ADMIN") || request.isUserInRole("OPERATOR")) {
            checkParamsWithRoles(message.getParams(), MAILBOX_POST, authentication);
        } else {
            checkParamsWithRolesAndDeleteRestricted(message.getParams(), MAILBOX_POST, authentication);
        }

        checkParamsForServicesOnUpdate(message.getParams(), accountManager.findOne(accountId));

        resourceChecker.checkResource(account, ResourceType.MAILBOX, message.getParams());

        ProcessingBusinessAction businessAction = businessHelper.buildActionAndOperation(BusinessOperationType.MAILBOX_CREATE, BusinessActionType.MAILBOX_CREATE_RC, message);

        response.setStatus(HttpServletResponse.SC_ACCEPTED);

        saveHistory(request, accountId, "Поступила заявка на создание почтового ящика (имя: " + message.getParam("name") + ")");

        return this.createSuccessResponse(businessAction);
    }

    @RequestMapping(value = "/{resourceId}", method = RequestMethod.PATCH)
    public SimpleServiceMessage update(
            @PathVariable String resourceId,
            @RequestBody SimpleServiceMessage message,
            HttpServletResponse response,
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            SecurityContextHolderAwareRequestWrapper request,
            Authentication authentication
    ) {
        message.setAccountId(accountId);
        message.getParams().put("resourceId", resourceId);

        logger.debug("Updating mailbox with id " + resourceId + " " + message.toString());

        PersonalAccount account = accountManager.findOne(accountId);

        if (!account.isActive()) {
            throw new ParameterValidationException("Аккаунт неактивен. Обновление почтового ящика невозможно.");
        }

        if (request.isUserInRole("ADMIN") || request.isUserInRole("OPERATOR")) {
            checkParamsWithRoles(message.getParams(), MAILBOX_PATCH, authentication);
        } else {
            checkParamsWithRolesAndDeleteRestricted(message.getParams(), MAILBOX_PATCH, authentication);
        }

        checkParamsForServicesOnUpdate(message.getParams(), accountManager.findOne(accountId));

        resourceChecker.checkResource(account, ResourceType.MAILBOX, message.getParams());

        ProcessingBusinessAction businessAction = businessHelper.buildActionAndOperation(BusinessOperationType.MAILBOX_UPDATE, BusinessActionType.MAILBOX_UPDATE_RC, message);

        response.setStatus(HttpServletResponse.SC_ACCEPTED);

        saveHistory(request, accountId, "Поступила заявка на обновление почтового ящика (Id: " + resourceId  + ", имя: " + message.getParam("name") + ")");

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

        logger.debug("Deleting mailbox with id " + resourceId + " " + message.toString());

        ProcessingBusinessAction businessAction = businessHelper.buildActionAndOperation(BusinessOperationType.MAILBOX_DELETE, BusinessActionType.MAILBOX_DELETE_RC, message);

        response.setStatus(HttpServletResponse.SC_ACCEPTED);

        saveHistory(request, accountId, "Поступила заявка на удаление почтового ящика (Id: " + resourceId  + ", имя: " + message.getParam("name") + ")");

        return this.createSuccessResponse(businessAction);
    }
}
