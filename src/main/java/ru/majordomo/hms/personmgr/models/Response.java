package ru.majordomo.hms.personmgr.models;


public class Response {

    private final String operationIdentity;
    private final Object content;

    public Response(String operationIdentity, Object content)
    {
        this.operationIdentity = operationIdentity;
        this.content = content;
    }

    public String getOperationIdentity() {
        return operationIdentity;
    }

    public Object getContent() {
        return content;
    }
}
