package ru.majordomo.hms.personmgr.model.business;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Objects;

import ru.majordomo.hms.personmgr.common.BusinessActionType;
import ru.majordomo.hms.personmgr.common.State;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.common.message.destination.GenericMessageDestination;

import javax.annotation.Nullable;

/**
 * BusinessAction
 *
 * Параметры для {@link ProcessingBusinessAction} которые нужно извлечь из бд по {@link BusinessAction#businessActionType}
 */
@Getter
@Setter
@Document
public class BusinessAction extends Step {

    /** {@link ProcessingBusinessOperation#getId()} */
    @Indexed
    @Nullable
    private String operationId;

    @Indexed
    private BusinessActionType businessActionType;

    private GenericMessageDestination destination;
    private SimpleServiceMessage message;

    public BusinessAction() {
    }

    @PersistenceConstructor
    public BusinessAction(String id, String name, State state, int priority, @Nullable String operationId, BusinessActionType businessActionType, GenericMessageDestination destination, SimpleServiceMessage message) {
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
