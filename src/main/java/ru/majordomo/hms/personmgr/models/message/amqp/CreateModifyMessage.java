package ru.majordomo.hms.personmgr.models.message.amqp;

import ru.majordomo.hms.personmgr.models.message.GenericMessage;

import java.util.Map;

public class CreateModifyMessage extends GenericMessage {

    private String objRef;
    private Map<Object, Object> params;

    public String getObjRef() {
        return objRef;
    }

    public void setObjRef(String objRef) {
        this.objRef = objRef;
    }

    public Map<Object, Object> getParams() {
        return params;
    }

    public void setParams(Map<Object, Object> params) {
        this.params = params;
    }

    public String toString() {
        return "operationIdentity: " + operationIdentity + ", objRef: " + objRef + ", params: " + params.toString();
    }
}
