package ru.majordomo.hms.personmgr.model;

import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

import ru.majordomo.hms.personmgr.common.FlowType;
import ru.majordomo.hms.personmgr.common.State;

/**
 * BusinessFlow
 */
@Document
public class BusinessFlow extends Step {
    @Indexed
    private FlowType flowType;

    @Transient
    private List<BusinessAction> businessActions = new ArrayList<>();

    public BusinessFlow() {
    }

    @PersistenceConstructor
    public BusinessFlow(String id, String name, State state, int priority, FlowType flowType) {
        super();
        this.setId(id);
        this.setName(name);
        this.setState(state);
        this.setPriority(priority);
        this.flowType = flowType;
    }

    public FlowType getFlowType() {
        return flowType;
    }

    public void setFlowType(FlowType flowType) {
        this.flowType = flowType;
    }

    public List<BusinessAction> getBusinessActions() {
        return businessActions;
    }

    public void setBusinessActions(List<BusinessAction> actions) {
        this.businessActions = actions;
    }

    public void addBusinessAction(BusinessAction action) {
        this.businessActions.add(action);
    }

    public void deleteBusinessAction(BusinessAction action) {
        this.businessActions.remove(action);
    }

    @Override
    public String toString() {
        return "BusinessFlow{" +
                "flowType=" + flowType +
                ", businessActions=" + businessActions +
                "} " + super.toString();
    }
}