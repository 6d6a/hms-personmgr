package ru.majordomo.hms.personmgr.common.message;

import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.util.Map;

public class ServiceMessage {
    private String operationIdentity;
    private String actionIdentity;
    private String objRef;
    private Map<Object,Object> params;

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

    public void addParam(Object name, Object value) {
        params.put(name,value);
    }

    public Map<Object, Object> getParams() {
        return params;
    }

    public void setParams(Map<Object, Object> params) {
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
        return "ServiceMessage{" +
                "operationIdentity='" + operationIdentity + '\'' +
                ", actionIdentity='" + actionIdentity + '\'' +
                ", objRef='" + objRef + '\'' +
                ", params=" + params +
                '}';
    }
}
