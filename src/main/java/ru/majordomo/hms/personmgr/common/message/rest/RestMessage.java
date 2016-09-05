package ru.majordomo.hms.personmgr.common.message.rest;

import java.util.HashMap;

import ru.majordomo.hms.personmgr.common.message.GenericMessage;

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
