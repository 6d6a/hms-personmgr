package ru.majordomo.hms.personmgr.controller.rest.resource;

import org.apache.commons.collections.MapUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestWrapper;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import ru.majordomo.hms.personmgr.common.*;
import ru.majordomo.hms.personmgr.common.message.*;
import ru.majordomo.hms.personmgr.controller.rest.CommonRestController;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.exception.ResourceIsLockedException;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessAction;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessOperation;
import ru.majordomo.hms.personmgr.validation.ObjectId;

import java.util.Collections;

import static ru.majordomo.hms.personmgr.common.FieldRoles.WEB_SITE_PATCH;
import static ru.majordomo.hms.personmgr.common.FieldRoles.WEB_SITE_POST;

@RestController
@RequestMapping("/{accountId}/website")
@Validated
public class WebSiteResourceRestController extends CommonRestController {

    @PostMapping
    public ResponseEntity<SimpleServiceMessage> create(
            @RequestBody SimpleServiceMessage message,
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            SecurityContextHolderAwareRequestWrapper request,
            Authentication authentication
    ) {
        message.setAccountId(accountId);

        logger.debug("Creating website: " + message.toString());

        PersonalAccount account = accountManager.findOne(accountId);

        if (!account.isActive() || account.isPreorder()) {
            throw new ParameterValidationException("Аккаунт неактивен. Создание сайта невозможно.");
        }

        if (account.isFreeze()) {
            throw new ParameterValidationException("Аккаунт заморожен. Создание сайта невозможно.");
        }

        if (!planCheckerService.canAddWebSite(accountId)) {
            throw new ParameterValidationException("Лимит тарифа на создание сайтов превышен");
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

        history.save(accountId, "Поступила заявка на создание сайта (имя: " + message.getParam("name") + ")", request);

        return ResponseEntity.accepted().body(createSuccessResponse(businessAction));
    }

    @PatchMapping("/{resourceId}")
    public ResponseEntity<SimpleServiceMessage> update(
            @PathVariable String resourceId,
            @RequestBody SimpleServiceMessage message,
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            SecurityContextHolderAwareRequestWrapper request,
            Authentication authentication
    ) {
        message.setAccountId(accountId);
        message.addParam("resourceId", resourceId);

        logger.debug("Updating website with id " + resourceId + " " + message.toString());

        PersonalAccount account = accountManager.findOne(accountId);

        if (!account.isActive() || account.isPreorder()) {
            throw new ParameterValidationException("Аккаунт неактивен. Изменение сайта невозможно.");
        }

        if (account.isFreeze()) {
            throw new ParameterValidationException("Аккаунт заморожен. Изменение сайта невозможно.");
        }

        if (request.isUserInRole("ADMIN") || request.isUserInRole("OPERATOR")) {
            checkParamsWithRoles(message.getParams(), WEB_SITE_PATCH, authentication);
        } else {
            checkParamsWithRolesAndDeleteRestricted(message.getParams(), WEB_SITE_PATCH, authentication);
        }

        resourceChecker.checkResource(account, ResourceType.WEB_SITE, message.getParams());

        ProcessingBusinessOperation operation;
        SimpleServiceMessage actionMessage = message;

        String extendedAction = MapUtils.getString(message.getParams(), ExtendedActionConstants.EXTENDED_ACTION_KEY, "");
        switch (extendedAction) {
            case "":
                operation = businessHelper.buildOperation(BusinessOperationType.WEB_SITE_UPDATE, message);
                break;

            case ExtendedActionConstants.LOAD_INSTALL_SHELL:
                actionMessage = new SimpleServiceMessage(message);
                actionMessage.addParam(ExtendedActionConstants.EXTENDED_ACTION_KEY, ExtendedActionConstants.LOAD_INSTALL);
                operation = businessHelper.buildOperation(BusinessOperationType.WEB_SITE_UPDATE_EXTENDED_ACTION, message, Collections.singletonMap(ExtendedActionConstants.EXTENDED_ACTION_KEY, extendedAction));
                businessHelper.setStage(operation.getId(), ExtendedActionStage.BEFORE_FULL_SHELL);
                break;

            case ExtendedActionConstants.LOAD_INSTALL_SHELLUPDATE:
                actionMessage = new SimpleServiceMessage(message);
                actionMessage.addParam(ExtendedActionConstants.EXTENDED_ACTION_KEY, ExtendedActionConstants.LOAD_INSTALL);
                operation = businessHelper.buildOperation(BusinessOperationType.WEB_SITE_UPDATE_EXTENDED_ACTION, message, Collections.singletonMap(ExtendedActionConstants.EXTENDED_ACTION_KEY, extendedAction));
                businessHelper.setStage(operation.getId(), ExtendedActionStage.BEFORE_FULL_SHELLUPDATE);
                break;

            default:
                operation = businessHelper.buildOperation(BusinessOperationType.WEB_SITE_UPDATE_EXTENDED_ACTION, message, Collections.singletonMap(ExtendedActionConstants.EXTENDED_ACTION_KEY, extendedAction));
                if (operation == null) {
                    throw new ResourceIsLockedException();
                }
        }
        ProcessingBusinessAction businessAction = businessHelper.buildActionByOperation(BusinessActionType.WEB_SITE_UPDATE_RC, actionMessage, operation);;

        history.save(accountId, "Поступила заявка на обновление сайта (Id: " + resourceId  + ", имя: " + message.getParam("name") + ")", request);

        return ResponseEntity.accepted().body(createSuccessResponse(businessAction));
    }

    @DeleteMapping("/{resourceId}")
    public ResponseEntity<SimpleServiceMessage> delete(
            @PathVariable String resourceId,
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

        history.save(accountId, "Поступила заявка на удаление сайта (Id: " + resourceId  + ", имя: " + message.getParam("name") + ")", request);

        return ResponseEntity.accepted().body(createSuccessResponse(businessAction));
    }
}
