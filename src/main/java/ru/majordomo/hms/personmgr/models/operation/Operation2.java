package ru.majordomo.hms.personmgr.models.operation;

import org.springframework.stereotype.Component;

import java.util.HashMap;

@Component
public class Operation2 {

    private String operationIdentity;
    private ObjRefLink objRefLink;
    private HashMap parameters;

    public String getOperationIdentity() {
        return operationIdentity;
    }

    public void setOperationIdentity(String operationIdentity) {
        this.operationIdentity = operationIdentity;
    }

    public ObjRefLink getObjRefLink() {
        return objRefLink;
    }

    public void setObjRefLink(ObjRefLink objRefLink) {
        this.objRefLink = objRefLink;
    }

    public HashMap getParameters() {
        return parameters;
    }

    public void setParameters(HashMap parameters) {
        this.parameters = parameters;
    }
}
