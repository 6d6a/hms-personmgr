package ru.majordomo.hms.personmgr.controller.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestWrapper;

import java.util.Map;
import java.util.Set;

import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.exception.ParameterWithRoleSecurityException;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessAction;

public class CommonRestController {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected PersonalAccountManager accountManager;
    protected ApplicationEventPublisher publisher;

    @Autowired
    public void setAccountManager(PersonalAccountManager accountManager) {
        this.accountManager = accountManager;
    }

    @Autowired
    public void setPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    private SimpleServiceMessage createResponse() {
        return new SimpleServiceMessage();
    }

    protected SimpleServiceMessage createSuccessResponse(ProcessingBusinessAction businessAction) {
        SimpleServiceMessage message = createResponse();
        message = fillStatus(message, true);
        message = fillFromBusinessAction(message, businessAction);

        return message;
    }

    protected SimpleServiceMessage createSuccessResponse(String successMessage) {
        SimpleServiceMessage message = createResponse();
        message = fillStatus(message, true);
        message = fillTextMessage(message, "successMessage", successMessage);

        return message;
    }

    protected SimpleServiceMessage createErrorResponse(String errorMessage) {
        SimpleServiceMessage message = createResponse();
        message = fillStatus(message, false);
        message = fillTextMessage(message, "errorMessage", errorMessage);

        return message;
    }

    private SimpleServiceMessage fillFromBusinessAction(SimpleServiceMessage message, ProcessingBusinessAction businessAction) {
        message.setActionIdentity(businessAction.getId());
        message.setOperationIdentity(businessAction.getOperationId());

        message.addParams(businessAction.getParams());

        return message;
    }

    private SimpleServiceMessage fillStatus(SimpleServiceMessage message, boolean success) {
        message.addParam("success", success);

        return message;
    }

    private SimpleServiceMessage fillTextMessage(SimpleServiceMessage message, String messageName, String messageText) {
        message.addParam(messageName, messageText);

        return message;
    }

    protected void checkRequiredParams(Map<String, Object> params, Set<String> requiredParams) {
        for (String field : requiredParams) {
            if (params.get(field) == null) {
                logger.debug("No " + field + " property found in request");
                throw new ParameterValidationException("No " + field + " property found in request");
            }
        }
    }

    protected void checkParamsWithRoles(Map<String, Object> params, Map<String, String> paramsWithRoles, SecurityContextHolderAwareRequestWrapper request) {
        paramsWithRoles.forEach((param, role) -> {
            if (params.get(param) != null && !request.isUserInRole(role)) {
                logger.debug("Changing '" + param + "' property is forbidden. Only role '" + role + "' allowed to edit.");
                throw new ParameterWithRoleSecurityException("Changing '" + param + "' property is forbidden");
            }
        });
    }
}
