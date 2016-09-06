package ru.majordomo.hms.personmgr.common.message.amqp;

import java.util.Map;

import ru.majordomo.hms.personmgr.common.message.GenericMessage;

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

    @Override
    public String toString() {
        return "CreateModifyMessage{" +
                "objRef='" + objRef + '\'' +
                ", params=" + params +
                "} " + super.toString();
    }
}
