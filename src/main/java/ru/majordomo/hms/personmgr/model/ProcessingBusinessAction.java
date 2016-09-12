package ru.majordomo.hms.personmgr.model;

import org.bson.types.ObjectId;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.type.MapType;
import org.codehaus.jackson.map.type.TypeFactory;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.PersistenceConstructor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import ru.majordomo.hms.personmgr.common.ActionType;
import ru.majordomo.hms.personmgr.common.State;
import ru.majordomo.hms.personmgr.common.message.ServiceMessage;
import ru.majordomo.hms.personmgr.common.message.destination.GenericMessageDestination;
import ru.majordomo.hms.personmgr.common.message.ServiceMessageParams;

/**
 * ProcessingBusinessAction
 */
public class ProcessingBusinessAction extends BusinessAction {
    @CreatedDate
    private LocalDateTime createdDate;

    @LastModifiedDate
    private LocalDateTime updatedDate;

    private ServiceMessageParams params;

    private Map<String,Object> mapParams;

    public ProcessingBusinessAction(BusinessAction businessAction) {
        super();
        this.setId(ObjectId.get().toHexString());
        this.setDestination(businessAction.getDestination());
        this.setName(businessAction.getName());
        this.setMessage(businessAction.getMessage());
        this.setPriority(businessAction.getPriority());
        this.setState(businessAction.getState());
    }

    @PersistenceConstructor
    public ProcessingBusinessAction(String id, String name, State state, int priority, String operationId, ActionType actionType, GenericMessageDestination destination, ServiceMessage message) {
        super(id, name, state, priority, operationId, actionType, destination, message);
    }

    public ServiceMessageParams getParams() {
        return params;
    }

    public void setParams(ServiceMessageParams params) {
        this.params = params;

        ObjectMapper mapper = new ObjectMapper();
        TypeFactory typeFactory = mapper.getTypeFactory();
        MapType mapType = typeFactory.constructMapType(HashMap.class, String.class, Object.class);
        this.mapParams = mapper.convertValue(params, mapType);
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public LocalDateTime getUpdatedDate() {
        return updatedDate;
    }

    public void setUpdatedDate(LocalDateTime updatedDate) {
        this.updatedDate = updatedDate;
    }

    public Map<String, Object> getMapParams() {
        return mapParams;
    }

    public void setMapParams(Map<String, Object> mapParams) {
        this.mapParams = mapParams;
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
