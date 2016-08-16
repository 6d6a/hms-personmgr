package ru.majordomo.hms.personmgr.models.message;

import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;

public class GenericMessage {
    protected String operationIdentity;

    public String getOperationIdentity() {
        return operationIdentity;
    }

    public void setOperationIdentity(String operationIdentity) {
        this.operationIdentity = operationIdentity;
    }

    public String toJson() {
        ObjectMapper objectMapper = new ObjectMapper();
        String message = "";
        try {
            message = objectMapper.writeValueAsString(this);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return message;
    }
}
