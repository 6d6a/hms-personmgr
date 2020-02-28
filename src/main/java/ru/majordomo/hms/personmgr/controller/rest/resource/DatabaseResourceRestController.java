package ru.majordomo.hms.personmgr.controller.rest.resource;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestWrapper;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import ru.majordomo.hms.personmgr.common.BusinessActionType;
import ru.majordomo.hms.personmgr.common.BusinessOperationType;
import ru.majordomo.hms.personmgr.common.ResourceType;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.controller.rest.CommonRestController;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessAction;
import ru.majordomo.hms.personmgr.validation.ObjectId;

import static ru.majordomo.hms.personmgr.common.FieldRoles.DATABASE_PATCH;
import static ru.majordomo.hms.personmgr.common.FieldRoles.DATABASE_POST;

@RestController
@RequestMapping("/{accountId}/database")
@Validated
public class DatabaseResourceRestController extends CommonRestController {

    @PostMapping
    public ResponseEntity<SimpleServiceMessage> create(
            @RequestBody SimpleServiceMessage message,
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            SecurityContextHolderAwareRequestWrapper request,
            Authentication authentication
    ) {
        message.setAccountId(accountId);

        logger.debug("Creating database " + message.toString());

        if (!planCheckerService.canAddDatabase(accountId)) {
            throw new ParameterValidationException("Лимит тарифа на базы данных превышен");
        }

        PersonalAccount account = accountManager.findOne(accountId);

        if (!account.isActive() || account.isPreorder()) {
            throw new ParameterValidationException("Аккаунт неактивен. Создание базы данных невозможно.");
        }

        if (account.isFreeze()) {
            throw new ParameterValidationException("Аккаунт заморожен. Создание базы данных невозможно.");
        }

        if (request.isUserInRole("ADMIN") || request.isUserInRole("OPERATOR")) {
            checkParamsWithRoles(message.getParams(), DATABASE_POST, authentication);
        } else {
            checkParamsWithRolesAndDeleteRestricted(message.getParams(), DATABASE_POST, authentication);
        }

        resourceChecker.checkResource(account, ResourceType.DATABASE, message.getParams());

        ProcessingBusinessAction businessAction = businessHelper.buildActionAndOperation(
                BusinessOperationType.DATABASE_CREATE,
                BusinessActionType.DATABASE_CREATE_RC,
                message
        );

        history.save(accountId, "Поступила заявка на создание базы данных (имя: " + message.getParam("name") + ")", request);

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

        logger.debug("Updating database with id " + resourceId + " " + message.toString());

        PersonalAccount account = accountManager.findOne(accountId);

        if (!account.isActive() || account.isPreorder()) {
            throw new ParameterValidationException("Аккаунт неактивен. Обновление базы данных невозможно.");
        }

        if (account.isFreeze()) {
            throw new ParameterValidationException("Аккаунт заморожен. Обновление базы данных невозможно.");
        }

        if (request.isUserInRole("ADMIN") || request.isUserInRole("OPERATOR")) {
            checkParamsWithRoles(message.getParams(), DATABASE_PATCH, authentication);
        } else {
            checkParamsWithRolesAndDeleteRestricted(message.getParams(), DATABASE_PATCH, authentication);
        }

        resourceChecker.checkResource(account, ResourceType.DATABASE, message.getParams());

        ProcessingBusinessAction businessAction = businessHelper.buildActionAndOperation(
                BusinessOperationType.DATABASE_UPDATE,
                BusinessActionType.DATABASE_UPDATE_RC,
                message
        );

        history.save(accountId,"Поступила заявка на обновление базы данных (Id: " + resourceId  + ", имя: " + message.getParam("name") + ")", request);

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

        logger.debug("Deleting database with id " + resourceId + " " + message.toString());

        ProcessingBusinessAction businessAction = businessHelper.buildActionAndOperation(
                BusinessOperationType.DATABASE_DELETE,
                BusinessActionType.DATABASE_DELETE_RC,
                message
        );

        history.save(accountId, "Поступила заявка на удаление базы данных (Id: " + resourceId  + ", имя: " + message.getParam("name") + ")", request);

        return ResponseEntity.accepted().body(createSuccessResponse(businessAction));
    }
}
