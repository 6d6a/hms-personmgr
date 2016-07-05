package ru.majordomo.hms.personmgr.models.message;

import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.util.HashMap;

public class RestMessage {

    private String operationIdentity;
    private HashMap<String, String> data;

    public RestMessage() {

    }

    public RestMessage(String operationIdentity, HashMap<String, String> data) {
        this.operationIdentity = operationIdentity;
        this.data = data;
    }

    public String getOperationIdentity() {
        return operationIdentity;
    }

    public void setOperationIdentity(String operationIdentity) {
        this.operationIdentity = operationIdentity;
    }

    public HashMap<String, String> getData() {
        return data;
    }

    public void setData(HashMap<String, String> data) {
        this.data = data;
    }

    public String toString() {
        return "RestObject: " + "data: " + data.toString();
    }
}
