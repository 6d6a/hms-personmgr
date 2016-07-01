package ru.majordomo.hms.personmgr.Test.helpers;

public class RestConsumer {

    private String content;
    private int operationIdentity;

    public RestConsumer() {
    }

    public int getOperationIdentity() {
        return operationIdentity;
    }

    public void setOperationIdentity(int operationIdentity) {
        this.operationIdentity = operationIdentity;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public String toString() {
        return "Response{" +
                "id='" + operationIdentity + '\'' +
                ", content='" + content +
                "'}";
    }

}
