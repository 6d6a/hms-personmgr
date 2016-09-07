package ru.majordomo.hms.personmgr.model;

import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import ru.majordomo.hms.personmgr.common.State;
import ru.majordomo.hms.personmgr.common.message.GenericMessageDestination;
import ru.majordomo.hms.personmgr.common.message.ServiceMessageParams;
import ru.majordomo.hms.personmgr.validators.ObjectId;

/**
 * BusinessAction
 */
@Document
public class BusinessAction extends Step {
    @Indexed
    @ObjectId(BusinessFlow.class)
    private String businessFlowId;
    private GenericMessageDestination destination;
    private String message;


    public String getBusinessFlowId() {
        return businessFlowId;
    }

    public void setBusinessFlowId(String businessFlowId) {
        this.businessFlowId = businessFlowId;
    }

    public GenericMessageDestination getDestination() {
        return destination;
    }

    public void setDestination(GenericMessageDestination destination) {
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
    public BusinessAction(String id, String name, State state, int priority, String businessFlowId, GenericMessageDestination destination, String message) {
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
                ", destination=" + destination +
                ", message='" + message + '\'' +
                "} " + super.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        BusinessAction that = (BusinessAction) o;
        return Objects.equals(businessFlowId, that.businessFlowId) &&
                Objects.equals(destination, that.destination) &&
                Objects.equals(message, that.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), businessFlowId, destination, message);
    }
}
