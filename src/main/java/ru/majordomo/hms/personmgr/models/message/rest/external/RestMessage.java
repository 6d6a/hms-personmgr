package ru.majordomo.hms.personmgr.models.message.rest.external;

import ru.majordomo.hms.personmgr.models.message.GenericMessage;

import java.util.HashMap;

public class RestMessage extends GenericMessage {

    private HashMap<Object, Object> data;

    public RestMessage() {

    }

    public RestMessage(String operationIdentity, HashMap<Object, Object> data) {
        this.operationIdentity = operationIdentity;
        this.data = data;
    }

    public HashMap<Object, Object> getData() {
        return data;
    }

    public void setData(HashMap<Object, Object> data) {
        this.data = data;
    }

    public String toString() {
        return "request: operationIdentity: " + operationIdentity + ", data: " + data.toString();
    }
}
