package ru.majordomo.hms.personmgr.controller.rest;

import org.springframework.web.bind.annotation.CrossOrigin;

import ru.majordomo.hms.personmgr.common.message.ResponseMessage;
import ru.majordomo.hms.personmgr.common.message.ResponseMessageParams;
import ru.majordomo.hms.personmgr.model.ProcessingBusinessAction;

@org.springframework.web.bind.annotation.RestController
@CrossOrigin("*")
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
