package ru.majordomo.hms.personmgr.model;

import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Objects;

import ru.majordomo.hms.personmgr.common.ActionType;
import ru.majordomo.hms.personmgr.common.State;
import ru.majordomo.hms.personmgr.common.message.ServiceMessage;
import ru.majordomo.hms.personmgr.common.message.destination.GenericMessageDestination;

/**
 * BusinessAction
 */
@Document
public class BusinessAction extends Step {
    @Indexed
    private String operationId;

    @Indexed
    private ActionType actionType;

    private GenericMessageDestination destination;
    private ServiceMessage message;

    public BusinessAction() {
    }

    @PersistenceConstructor
    public BusinessAction(String id, String name, State state, int priority, String operationId, ActionType actionType, GenericMessageDestination destination, ServiceMessage message) {
        super();
        this.setId(id);
        this.setName(name);
        this.setState(state);
        this.setPriority(priority);
        this.operationId = operationId;
        this.actionType = actionType;
        this.destination = destination;
        this.message = message;
    }

    public String getOperationId() {
        return operationId;
    }

    public void setOperationId(String operationId) {
        this.operationId = operationId;
    }

    public ActionType getActionType() {
        return actionType;
    }

    public void setActionType(ActionType actionType) {
        this.actionType = actionType;
    }

    public GenericMessageDestination getDestination() {
        return destination;
    }

    public void setDestination(GenericMessageDestination destination) {
        this.destination = destination;
    }

    public ServiceMessage getMessage() {
        return message;
    }

    public void setMessage(ServiceMessage message) {
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
