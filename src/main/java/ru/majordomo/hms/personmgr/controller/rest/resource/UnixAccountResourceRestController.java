package ru.majordomo.hms.personmgr.controller.rest.resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestWrapper;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

import ru.majordomo.hms.personmgr.common.BusinessActionType;
import ru.majordomo.hms.personmgr.common.BusinessOperationType;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.controller.rest.CommonRestController;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessAction;
import ru.majordomo.hms.personmgr.feign.RcUserFeignClient;
import ru.majordomo.hms.personmgr.validation.ObjectId;
import ru.majordomo.hms.rc.user.resources.UnixAccount;

import static ru.majordomo.hms.personmgr.common.FieldRoles.UNIX_ACCOUNT_PATCH;
import static ru.majordomo.hms.personmgr.common.FieldRoles.UNIX_ACCOUNT_POST;

@RestController
@RequestMapping("/{accountId}/unix-account")
@Validated
public class UnixAccountResourceRestController extends CommonRestController {
    private final RcUserFeignClient rcUserFeignClient;

    @Autowired
    public UnixAccountResourceRestController(
            RcUserFeignClient rcUserFeignClient
    ) {
        this.rcUserFeignClient = rcUserFeignClient;
    }

    @PostMapping
    public ResponseEntity<SimpleServiceMessage> create(
            @RequestBody SimpleServiceMessage message,
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            SecurityContextHolderAwareRequestWrapper request,
            Authentication authentication
    ) {
        message.setAccountId(accountId);

        Collection<UnixAccount> unixAccounts = rcUserFeignClient.getUnixAccounts(accountId);

        if (!accountManager.findOne(accountId).isActive() && (unixAccounts != null && !unixAccounts.isEmpty())) {
            throw new ParameterValidationException("Аккаунт неактивен. Создание Unix аккаунта невозможно.");
        }

        logger.debug("Creating unix account " + message.toString());

        if (request.isUserInRole("ADMIN") || request.isUserInRole("OPERATOR")) {
            checkParamsWithRoles(message.getParams(), UNIX_ACCOUNT_POST, authentication);
        } else {
            checkParamsWithRolesAndDeleteRestricted(message.getParams(), UNIX_ACCOUNT_POST, authentication);
        }

        ProcessingBusinessAction businessAction = businessHelper.buildActionAndOperation(BusinessOperationType.UNIX_ACCOUNT_CREATE, BusinessActionType.UNIX_ACCOUNT_CREATE_RC, message);

        history.save(accountId, "Поступила заявка на создание Unix-аккаунта (имя: " + message.getParam("name") + ")", request);

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

        if (!accountManager.findOne(accountId).isActive()) {
            throw new ParameterValidationException("Аккаунт неактивен. Обновление Unix аккаунта невозможно.");
        }

        logger.debug("Updating unix account with id " + resourceId + " " + message.toString());

        if (request.isUserInRole("ADMIN") || request.isUserInRole("OPERATOR")) {
            checkParamsWithRoles(message.getParams(), UNIX_ACCOUNT_PATCH, authentication);
        } else {
            checkParamsWithRolesAndDeleteRestricted(message.getParams(), UNIX_ACCOUNT_PATCH, authentication);
        }

        ProcessingBusinessAction businessAction = businessHelper.buildActionAndOperation(BusinessOperationType.UNIX_ACCOUNT_UPDATE, BusinessActionType.UNIX_ACCOUNT_UPDATE_RC, message);

        history.save(accountId, "Поступила заявка на обновление Unix-аккаунта (Id: " + resourceId  + ", имя: " + message.getParam("name") + ")", request);

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

        logger.debug("Deleting unix account with id " + resourceId + " " + message.toString());

        ProcessingBusinessAction businessAction = businessHelper.buildActionAndOperation(BusinessOperationType.UNIX_ACCOUNT_DELETE, BusinessActionType.UNIX_ACCOUNT_DELETE_RC, message);

        history.save(accountId, "Поступила заявка на удаление Unix-аккаунта (Id: " + resourceId  + ", имя: " + message.getParam("name") + ")", request);

        return ResponseEntity.accepted().body(createSuccessResponse(businessAction));
    }
}
