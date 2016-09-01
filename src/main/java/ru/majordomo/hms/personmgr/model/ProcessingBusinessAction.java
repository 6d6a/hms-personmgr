package ru.majordomo.hms.personmgr.model;

import org.springframework.data.annotation.PersistenceConstructor;

import ru.majordomo.hms.personmgr.common.State;

/**
 * ProcessingBusinessAction
 */
public class ProcessingBusinessAction extends BusinessAction {
    public ProcessingBusinessAction(BusinessAction businessAction) {
        super();
        this.setDestination(businessAction.getDestination());
        this.setName(businessAction.getName());
        this.setMessage(businessAction.getMessage());
        this.setPriority(businessAction.getPriority());
        this.setState(businessAction.getState());
    }

    @PersistenceConstructor
    public ProcessingBusinessAction(String id, String name, State state, int priority, String businessFlowId, String destination, String message) {
        super(id, name, state, priority, businessFlowId, destination, message);
    }
}
