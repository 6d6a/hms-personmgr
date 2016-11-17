package ru.majordomo.hms.personmgr.controller.rest;

import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.model.ProcessingBusinessAction;

public class CommonRestController {
    private SimpleServiceMessage createResponse() {
        return new SimpleServiceMessage();
    }

    protected SimpleServiceMessage createSuccessResponse(ProcessingBusinessAction businessAction) {
        SimpleServiceMessage message = createResponse();
        message = fillStatus(message, true);
        message = fillFromBusinessAction(message, businessAction);

        return message;
    }

    protected SimpleServiceMessage createErrorResponse(String errorMessage) {
        SimpleServiceMessage message = createResponse();
        message = fillStatus(message, false);
        message = fillErrorMessage(message, errorMessage);

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

    private SimpleServiceMessage fillErrorMessage(SimpleServiceMessage message, String errorMessage) {
        message.addParam("errorMessage", errorMessage);

        return message;
    }
}
