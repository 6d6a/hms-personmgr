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
import ru.majordomo.hms.personmgr.common.message.*;
import ru.majordomo.hms.personmgr.controller.rest.CommonRestController;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessAction;
import ru.majordomo.hms.personmgr.validation.ObjectId;

import static ru.majordomo.hms.personmgr.common.FieldRoles.WEB_SITE_PATCH;
import static ru.majordomo.hms.personmgr.common.FieldRoles.WEB_SITE_POST;

@RestController
@RequestMapping("/{accountId}/website")
@Validated
public class WebSiteResourceRestController extends CommonRestController {
    @RequestMapping(value = "", method = RequestMethod.POST)
    public SimpleServiceMessage create(
            @RequestBody SimpleServiceMessage message,
            HttpServletResponse response,
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            SecurityContextHolderAwareRequestWrapper request,
            Authentication authentication
    ) {
        message.setAccountId(accountId);

        logger.debug("Creating website: " + message.toString());

        PersonalAccount account = accountManager.findOne(accountId);

        if (!account.isActive()) {
            throw new ParameterValidationException("Аккаунт неактивен. Создание сайта невозможно.");
        }

        if (!planCheckerService.canAddWebSite(accountId)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);

            return this.createErrorResponse("Plan limit for websites exceeded");
        }

        if (request.isUserInRole("ADMIN") || request.isUserInRole("OPERATOR")) {
            checkParamsWithRoles(message.getParams(), WEB_SITE_POST, authentication);
        } else {
            checkParamsWithRolesAndDeleteRestricted(message.getParams(), WEB_SITE_POST, authentication);
        }

        resourceChecker.checkResource(account, ResourceType.WEB_SITE, message.getParams());

        ProcessingBusinessAction businessAction = businessHelper.buildActionAndOperation(
                BusinessOperationType.WEB_SITE_CREATE,
                BusinessActionType.WEB_SITE_CREATE_RC,
                message
        );

        response.setStatus(HttpServletResponse.SC_ACCEPTED);

        saveHistory(request, accountId, "Поступила заявка на создание сайта (имя: " + message.getParam("name") + ")");

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
        message.addParam("resourceId", resourceId);

        logger.debug("Updating website with id " + resourceId + " " + message.toString());

        PersonalAccount account = accountManager.findOne(accountId);

        if (!account.isActive()) {
            throw new ParameterValidationException("Аккаунт неактивен. Создание сайта невозможно.");
        }

        if (request.isUserInRole("ADMIN") || request.isUserInRole("OPERATOR")) {
            checkParamsWithRoles(message.getParams(), WEB_SITE_PATCH, authentication);
        } else {
            checkParamsWithRolesAndDeleteRestricted(message.getParams(), WEB_SITE_PATCH, authentication);
        }

        resourceChecker.checkResource(account, ResourceType.WEB_SITE, message.getParams());

        ProcessingBusinessAction businessAction = businessHelper.buildActionAndOperation(
                BusinessOperationType.WEB_SITE_UPDATE,
                BusinessActionType.WEB_SITE_UPDATE_RC,
                message
        );

        response.setStatus(HttpServletResponse.SC_ACCEPTED);

        saveHistory(request, accountId, "Поступила заявка на обновление сайта (Id: " + resourceId  + ", имя: " + message.getParam("name") + ")");

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

        logger.debug("Deleting website with id " + resourceId + " " + message.toString());

        ProcessingBusinessAction businessAction = businessHelper.buildActionAndOperation(
                BusinessOperationType.WEB_SITE_DELETE,
                BusinessActionType.WEB_SITE_DELETE_RC,
                message
        );

        response.setStatus(HttpServletResponse.SC_ACCEPTED);

        saveHistory(request, accountId, "Поступила заявка на удаление сайта (Id: " + resourceId  + ", имя: " + message.getParam("name") + ")");

        return this.createSuccessResponse(businessAction);
    }
}
