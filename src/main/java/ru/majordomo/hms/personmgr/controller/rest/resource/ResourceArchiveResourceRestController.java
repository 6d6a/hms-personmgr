package ru.majordomo.hms.personmgr.controller.rest.resource;

import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestWrapper;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import ru.majordomo.hms.personmgr.common.BusinessActionType;
import ru.majordomo.hms.personmgr.common.BusinessOperationType;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.controller.rest.CommonRestController;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessAction;
import ru.majordomo.hms.personmgr.validation.ObjectId;

import javax.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/{accountId}/resource-archive")
@Validated
public class ResourceArchiveResourceRestController extends CommonRestController {
    @RequestMapping(value = "", method = RequestMethod.POST)
    public SimpleServiceMessage create(
            @RequestBody SimpleServiceMessage message,
            HttpServletResponse response,
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            SecurityContextHolderAwareRequestWrapper request
    ) {
        message.setAccountId(accountId);

        logger.debug("Creating Resource Archive. Message: " + message.toString());

        ProcessingBusinessAction businessAction = businessHelper.buildActionAndOperation(BusinessOperationType.RESOURCE_ARCHIVE_CREATE, BusinessActionType.RESOURCE_ARCHIVE_CREATE_RC, message);

        response.setStatus(HttpServletResponse.SC_ACCEPTED);

        saveHistory(request, accountId, "Поступила заявка на создание архива (имя: " + message.getParam("name") + ")");

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

        logger.debug("Updating Resource Archive with id " + resourceId + " " + message.toString());

        ProcessingBusinessAction businessAction = businessHelper.buildActionAndOperation(BusinessOperationType.RESOURCE_ARCHIVE_UPDATE, BusinessActionType.RESOURCE_ARCHIVE_UPDATE_RC, message);

        response.setStatus(HttpServletResponse.SC_ACCEPTED);

        saveHistory(request, accountId, "Поступила заявка на обновление архива (Id: " + resourceId  + ", имя: " + message.getParam("name") + ")");

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

        logger.debug("Deleting Resource Archive with id " + resourceId + " " + message.toString());

        ProcessingBusinessAction businessAction = businessHelper.buildActionAndOperation(BusinessOperationType.RESOURCE_ARCHIVE_DELETE, BusinessActionType.RESOURCE_ARCHIVE_DELETE_RC, message);

        response.setStatus(HttpServletResponse.SC_ACCEPTED);

        saveHistory(request, accountId, "Поступила заявка на удаление архива (Id: " + resourceId  + ", имя: " + message.getParam("name") + ")");

        return this.createSuccessResponse(businessAction);
    }
}
