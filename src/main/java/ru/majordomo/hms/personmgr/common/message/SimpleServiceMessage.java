package ru.majordomo.hms.personmgr.common.message;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NoArgsConstructor;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@NoArgsConstructor
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

    public SimpleServiceMessage(String accountId, @Nullable String operationIdentity, @Nullable String actionIdentity) {
        this.operationIdentity = operationIdentity;
        this.actionIdentity = actionIdentity;
        this.accountId = accountId;
    }

    public void addParams(Map<String, Object> params) {
        this.params.putAll(params);
    }

    public SimpleServiceMessage addParam(String name, Object value) {
        if (params == null) {
            params = new HashMap<>();
        }

        params.put(name,value);
        return this;
    }

    public void removeParam(String key) {
        if (params != null) {
            params.remove(key);
        }
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

    public SimpleServiceMessage withAccountId(String accountId) {
        this.accountId = accountId;
        return this;
    }

    public SimpleServiceMessage withParam(String key, Object value) {
        if (params == null) {
            params = new HashMap<>();
        }
        params.put(key, value);
        return this;
    }

    @Override
    public String toString() {
        return "SimpleServiceMessage{" +
                "operationIdentity='" + operationIdentity + '\'' +
                ", actionIdentity='" + actionIdentity + '\'' +
                ", accountId='" + accountId + '\'' +
                ", objRef='" + objRef + '\'' +
                ", params=" + params +
                '}';
    }
}