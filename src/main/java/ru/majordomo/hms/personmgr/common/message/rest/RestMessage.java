package ru.majordomo.hms.personmgr.common.message.rest;

import java.util.HashMap;

import ru.majordomo.hms.personmgr.common.message.GenericMessage;

public class RestMessage extends GenericMessage {

    private HashMap<Object, Object> params;

    public RestMessage() {

    }

    public RestMessage(String operationIdentity, HashMap<Object, Object> params) {
        this.operationIdentity = operationIdentity;
        this.params = params;
    }

    public HashMap<Object, Object> getParams() {
        return params;
    }

    public void setParams(HashMap<Object, Object> params) {
        this.params = params;
    }

    public String toString() {
        return "request: operationIdentity: " + operationIdentity + ", params: " + params.toString();
    }
}
