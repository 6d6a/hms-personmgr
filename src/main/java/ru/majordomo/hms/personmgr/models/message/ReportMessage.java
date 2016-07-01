package ru.majordomo.hms.personmgr.models.message;

/**
 * Created by dna on 01.07.16.
 */
public class ReportMessage {

    private String operationIdentity;
    private Boolean result;

    public String getOperationIdentity() {
        return operationIdentity;
    }

    public void setOperationIdentity(String operationIdentity) {
        this.operationIdentity = operationIdentity;
    }

    public Boolean getResult() {
        return result;
    }

    public void setResult(Boolean result) {
        this.result = result;
    }

    public String toString() {
        return "Message: " + "operationIdentity: " + operationIdentity + ", result: " + result;
    }
}
