package ru.majordomo.hms.personmgr.common.message;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class SimpleServiceMessage {
    private static final Logger logger = LoggerFactory.getLogger(SimpleServiceMessage.class);
    private String operationIdentity;
    private String actionIdentity;
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

    public String toJson() {
        ObjectMapper objectMapper = new ObjectMapper();
        String jsonData = "";
        try {
            jsonData = objectMapper.writeValueAsString(this);
        } catch (IOException ex) {
            logger.error("Невозможно конвертировать в JSON" + ex.toString());
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