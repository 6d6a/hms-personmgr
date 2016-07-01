package ru.majordomo.hms.personmgr.controller;


public class Response {

    private final String operationIdentity;
    private final String content;

    public Response(String operationIdentity, String content)
    {
        this.operationIdentity = operationIdentity;
        this.content = content;
    }

    public String getOperationIdentity() {
        return operationIdentity;
    }

    public String getContent() {
        return content;
    }
}
