package ru.majordomo.hms.personmgr.common;


public class RestResponse {

    private final String operationIdentity;
    private final Object content;

    public RestResponse(String operationIdentity, Object content)
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
