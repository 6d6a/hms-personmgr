package ru.majordomo.hms.personmgr.model;

import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.mongodb.core.mapping.Document;

import ru.majordomo.hms.personmgr.common.State;
import ru.majordomo.hms.personmgr.validators.ObjectId;

/**
 * BusinessAction
 */
@Document
public class BusinessAction extends Step {
    @ObjectId(BusinessFlow.class)
    private String businessFlowId;
    private String destination;
    private String message;

    public String getBusinessFlowId() {
        return businessFlowId;
    }

    public void setBusinessFlowId(String businessFlowId) {
        this.businessFlowId = businessFlowId;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public BusinessAction() {
    }

    @PersistenceConstructor
    public BusinessAction(String id, String name, State state, int priority, String businessFlowId, String destination, String message) {
        super();
        this.setId(id);
        this.setName(name);
        this.setState(state);
        this.setPriority(priority);
        this.businessFlowId = businessFlowId;
        this.destination = destination;
        this.message = message;
    }

    @Override
    public String toString() {
        return "BusinessAction{" +
                "businessFlowId='" + businessFlowId + '\'' +
                ", destination='" + destination + '\'' +
                ", message='" + message + '\'' +
                "} " + super.toString();
    }
}
