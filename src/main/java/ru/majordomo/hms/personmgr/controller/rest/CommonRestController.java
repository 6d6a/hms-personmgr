package ru.majordomo.hms.personmgr.controller.rest;

import ru.majordomo.hms.personmgr.common.message.ResponseMessage;
import ru.majordomo.hms.personmgr.common.message.ResponseMessageParams;
import ru.majordomo.hms.personmgr.model.ProcessingBusinessAction;

public class CommonRestController {
    protected ResponseMessage createResponse(ProcessingBusinessAction businessAction) {
        ResponseMessage responseMessage = new ResponseMessage();
        ResponseMessageParams messageParams = new ResponseMessageParams();
        messageParams.setSuccess(true);
        responseMessage.setParams(messageParams);
        responseMessage.setActionIdentity(businessAction.getId());
        responseMessage.setOperationIdentity(businessAction.getOperationId());

        return responseMessage;
    }
}
