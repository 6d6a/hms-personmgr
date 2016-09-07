package ru.majordomo.hms.personmgr.common.message;

import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.util.Map;

abstract class AbstractServiceMessage<T extends ServiceMessageParams> {
    private String operationIdentity;
    private String actionIdentity;
    private String objRef;
    private T params;

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

    public String getObjRef() {
        return objRef;
    }

    public void setObjRef(String objRef) {
        this.objRef = objRef;
    }

    public T getParams() {
        return params;
    }

    public void setParams(T params) {
        this.params = params;
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
        return "AbstractServiceMessage{" +
                "operationIdentity='" + operationIdentity + '\'' +
                ", actionIdentity='" + actionIdentity + '\'' +
                ", objRef='" + objRef + '\'' +
                ", params=" + params +
                '}';
    }
}
