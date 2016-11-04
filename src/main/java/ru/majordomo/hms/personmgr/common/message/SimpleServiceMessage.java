package ru.majordomo.hms.personmgr.common.message;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class SimpleServiceMessage {
    private String operationIdentity;
    private String actionIdentity;
    private String accountId;
    private String objRef;
    private Map<String, Object> params = new HashMap<>();

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

    public Object getParam(String param) {
        return params.get(param);
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public void setParams(Map<String, Object> params) {
        this.params = params;
    }

    public void addParam(String name, Object value) {
        params.put(name,value);
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String toJson() {
        ObjectMapper objectMapper = new ObjectMapper();
        String jsonData = "";
        try {
            jsonData = objectMapper.writeValueAsString(this);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return jsonData;
    }

    @Override
    public String toString() {
        return "ServiceMessage{" +
                "operationIdentity='" + operationIdentity + '\'' +
                ", actionIdentity='" + actionIdentity + '\'' +
                ", objRef='" + objRef + '\'' +
                ", params=" + params +
                '}';
    }
}