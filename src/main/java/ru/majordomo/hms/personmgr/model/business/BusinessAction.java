package ru.majordomo.hms.personmgr.model.business;

import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Objects;

import ru.majordomo.hms.personmgr.common.BusinessActionType;
import ru.majordomo.hms.personmgr.common.State;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.common.message.destination.GenericMessageDestination;

/**
 * BusinessAction
 */
@Document
public class BusinessAction extends Step {
    @Indexed
    private String operationId;

    @Indexed
    private BusinessActionType businessActionType;

    private GenericMessageDestination destination;
    private SimpleServiceMessage message;

    public BusinessAction() {
    }

    @PersistenceConstructor
    public BusinessAction(String id, String name, State state, int priority, String operationId, BusinessActionType businessActionType, GenericMessageDestination destination, SimpleServiceMessage message) {
        super();
        this.setId(id);
        this.setName(name);
        this.setState(state);
        this.setPriority(priority);
        this.operationId = operationId;
        this.businessActionType = businessActionType;
        this.destination = destination;
        this.message = message;
    }

    public String getOperationId() {
        return operationId;
    }

    public void setOperationId(String operationId) {
        this.operationId = operationId;
    }

    public BusinessActionType getBusinessActionType() {
        return businessActionType;
    }

    public void setBusinessActionType(BusinessActionType businessActionType) {
        this.businessActionType = businessActionType;
    }

    public GenericMessageDestination getDestination() {
        return destination;
    }

    public void setDestination(GenericMessageDestination destination) {
        this.destination = destination;
    }

    public SimpleServiceMessage getMessage() {
        return message;
    }

    public void setMessage(SimpleServiceMessage message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "BusinessAction{" +
                "operationId='" + operationId + '\'' +
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
        return Objects.equals(operationId, that.operationId) &&
                Objects.equals(destination, that.destination) &&
                Objects.equals(message, that.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), operationId, destination, message);
    }
}
