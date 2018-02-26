package ru.majordomo.hms.personmgr.controller.rest.resource;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestWrapper;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import ru.majordomo.hms.personmgr.common.BusinessActionType;
import ru.majordomo.hms.personmgr.common.BusinessOperationType;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.controller.rest.CommonRestController;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessAction;
import ru.majordomo.hms.personmgr.service.RcUserFeignClient;
import ru.majordomo.hms.personmgr.validation.ObjectId;
import ru.majordomo.hms.rc.user.resources.Person;

@RestController
@RequestMapping("/{accountId}/person")
@Validated
public class PersonResourceRestController extends CommonRestController {
    private final RcUserFeignClient rcUserFeignClient;

    public PersonResourceRestController(
            RcUserFeignClient rcUserFeignClient
    ) {
        this.rcUserFeignClient = rcUserFeignClient;
    }

    @RequestMapping(value = "", method = RequestMethod.POST)
    public SimpleServiceMessage create(
            @RequestBody SimpleServiceMessage message,
            HttpServletResponse response,
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            SecurityContextHolderAwareRequestWrapper request
    ) {
        message.setAccountId(accountId);

        logger.debug("Creating person " + message.toString());

        ProcessingBusinessAction businessAction = businessHelper.buildActionAndOperation(BusinessOperationType.PERSON_CREATE, BusinessActionType.PERSON_CREATE_RC, message);

        response.setStatus(HttpServletResponse.SC_ACCEPTED);

        saveHistory(request, accountId, "Поступила заявка на создание персоны (имя: " + message.getParam("name") + ")");

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
        message.addParam("resourceId", resourceId);

        logger.debug("Updating person with id " + resourceId + " " + message.toString());

        ProcessingBusinessAction businessAction = businessHelper.buildActionAndOperation(BusinessOperationType.PERSON_UPDATE, BusinessActionType.PERSON_UPDATE_RC, message);

        response.setStatus(HttpServletResponse.SC_ACCEPTED);

        saveHistory(request, accountId, "Поступила заявка на обновление персоны (Id: " + resourceId  + ", имя: " + message.getParam("name") + ")");

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

        logger.debug("Deleting person with id " + resourceId + " " + message.toString());

        PersonalAccount account = accountManager.findOne(message.getAccountId());

        //Если пытаемся удалить персону "владельца аккаунта"
        if (account != null && account.getOwnerPersonId() != null && account.getOwnerPersonId().equals(resourceId)) {
            this.createErrorResponse("Person with id " + resourceId + " is set as accountOwner and prohibited to delete");
        }

        ProcessingBusinessAction businessAction = businessHelper.buildActionAndOperation(BusinessOperationType.PERSON_DELETE, BusinessActionType.PERSON_DELETE_RC, message);

        response.setStatus(HttpServletResponse.SC_ACCEPTED);

        saveHistory(request, accountId, "Поступила заявка на удаление персоны (Id: " + resourceId  + ", имя: " + message.getParam("name") + ")");

        return this.createSuccessResponse(businessAction);
    }

    @PreAuthorize("hasAuthority('MANAGE_PERSONS')")
    @RequestMapping(value = "/add", method = RequestMethod.POST)
    public Person create(
            @RequestBody Map<String, String> requestBody,
            HttpServletResponse response,
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            SecurityContextHolderAwareRequestWrapper request
    ) {
        String nicHandle = requestBody.get("nicHandle");

        if (nicHandle == null || nicHandle.equals("")) {
            throw new ParameterValidationException("Для добавления персоны необходимо указать её nicHandle");
        }

        logger.debug("Adding person by nicHandle: " + nicHandle);

        return rcUserFeignClient.addPersonByNicHandle(accountId, requestBody);
    }
}
