package ru.majordomo.hms.personmgr.controller.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;

import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.controller.rest.resource.AccountResourceRestController;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.model.ProcessingBusinessAction;

import static ru.majordomo.hms.personmgr.common.RequiredField.ACCOUNT_CREATE;

public class CommonRestController {
    private final static Logger logger = LoggerFactory.getLogger(CommonRestController.class);

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
}
