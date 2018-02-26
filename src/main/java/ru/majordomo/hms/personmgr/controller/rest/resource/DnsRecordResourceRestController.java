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
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.controller.rest.CommonRestController;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessAction;
import ru.majordomo.hms.personmgr.validation.ObjectId;

import static ru.majordomo.hms.personmgr.common.FieldRoles.DNS_RECORD_PATCH;

@RestController
@RequestMapping("/{accountId}/dns-record")
@Validated
public class DnsRecordResourceRestController extends CommonRestController {
    @RequestMapping(value = "", method = RequestMethod.POST)
    public SimpleServiceMessage create(
            @RequestBody SimpleServiceMessage message,
            HttpServletResponse response,
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            SecurityContextHolderAwareRequestWrapper request
    ) {
        message.setAccountId(accountId);

        logger.debug("Creating DnsRecord. Message: " + message.toString());

        if (!accountManager.findOne(accountId).isActive()) {
            throw new ParameterValidationException("Аккаунт неактивен. Создание DNS-записи невозможно.");
        }

        ProcessingBusinessAction businessAction = businessHelper.buildActionAndOperation(BusinessOperationType.DNS_RECORD_CREATE, BusinessActionType.DNS_RECORD_CREATE_RC, message);

        response.setStatus(HttpServletResponse.SC_ACCEPTED);

        saveHistory(request, accountId, "Поступила заявка на создание днс-записи (имя: " + message.getParam("name") + ")");

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

        logger.debug("Updating DnsRecord with id " + resourceId + " " + message.toString());

        if (!accountManager.findOne(accountId).isActive()) {
            throw new ParameterValidationException("Аккаунт неактивен. Обновление DNS-записи невозможно.");
        }

        checkParamsWithRoles(message.getParams(), DNS_RECORD_PATCH, authentication);

        ProcessingBusinessAction businessAction = businessHelper.buildActionAndOperation(BusinessOperationType.DNS_RECORD_UPDATE, BusinessActionType.DNS_RECORD_UPDATE_RC, message);

        response.setStatus(HttpServletResponse.SC_ACCEPTED);

        saveHistory(request, accountId, "Поступила заявка на обновление днс-записи (Id: " + resourceId  + ", имя: " + message.getParam("name") + ")");//Save history

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

        logger.debug("Deleting DnsRecord with id " + resourceId + " " + message.toString());

        ProcessingBusinessAction businessAction = businessHelper.buildActionAndOperation(BusinessOperationType.DNS_RECORD_DELETE, BusinessActionType.DNS_RECORD_DELETE_RC, message);

        response.setStatus(HttpServletResponse.SC_ACCEPTED);

        saveHistory(request, accountId, "Поступила заявка на удаление днс-записи (Id: " + resourceId  + ", имя: " + message.getParam("name") + ")");

        return this.createSuccessResponse(businessAction);
    }
}
