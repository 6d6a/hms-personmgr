package ru.majordomo.hms.personmgr.common.message;

import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;

public class GenericMessage {
    protected String operationIdentity;

    protected String actionIdentity;

    public String getOperationIdentity() {
        return operationIdentity;
    }

    public void setOperationIdentity(String operationIdentity) {
        this.operationIdentity = operationIdentity;
    }

    public String getActionIdentity() {
        return actionIdentity;
    }

    public void setActionIdentity(String actionIdentity) {
        this.actionIdentity = actionIdentity;
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

    @Override
    public String toString() {
        return "GenericMessage{" +
                "operationIdentity='" + operationIdentity + '\'' +
                ", actionIdentity='" + actionIdentity + '\'' +
                '}';
    }
}
