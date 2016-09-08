package ru.majordomo.hms.personmgr.model;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.PersistenceConstructor;

import ru.majordomo.hms.personmgr.common.State;
import ru.majordomo.hms.personmgr.common.message.ServiceMessage;
import ru.majordomo.hms.personmgr.common.message.GenericMessageDestination;
import ru.majordomo.hms.personmgr.common.message.ServiceMessageParams;

/**
 * ProcessingBusinessAction
 */
public class ProcessingBusinessAction extends BusinessAction {
    private ServiceMessageParams params;

    public ProcessingBusinessAction(BusinessAction businessAction) {
        super();
        this.setId(ObjectId.get().toHexString());
        this.setBusinessFlowId("");
        this.setDestination(businessAction.getDestination());
        this.setName(businessAction.getName());
        this.setMessage(businessAction.getMessage());
        this.setPriority(businessAction.getPriority());
        this.setState(businessAction.getState());
    }

    @PersistenceConstructor
    public ProcessingBusinessAction(String id, String name, State state, int priority, String businessFlowId, GenericMessageDestination destination, ServiceMessage message) {
        super(id, name, state, priority, businessFlowId, destination, message);
    }

    public ServiceMessageParams getParams() {
        return params;
    }

    public void setParams(ServiceMessageParams params) {
        this.params = params;
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public String toString() {
        return "ProcessingBusinessAction{} " + super.toString();
    }
}
