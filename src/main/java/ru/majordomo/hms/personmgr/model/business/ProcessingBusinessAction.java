package ru.majordomo.hms.personmgr.model.business;

import com.fasterxml.jackson.annotation.JsonView;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.data.mongodb.core.mapping.Document;
import ru.majordomo.hms.personmgr.common.BusinessActionType;
import ru.majordomo.hms.personmgr.common.State;
import ru.majordomo.hms.personmgr.common.Views;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.common.message.destination.GenericMessageDestination;

/**
 * ProcessingBusinessAction
 */
@Document
public class ProcessingBusinessAction extends BusinessAction {
    @Indexed
    private String personalAccountId;

    @CreatedDate
    private LocalDateTime createdDate;

    @LastModifiedDate
    private LocalDateTime updatedDate;

    @JsonView(Views.Internal.class)
    private Map<String,Object> params = new HashMap<>();

    public ProcessingBusinessAction(BusinessAction businessAction) {
        super();
//        this.setOperationId(ObjectId.get().toHexString());
        this.setDestination(businessAction.getDestination());
        this.setName(businessAction.getName());
        this.setMessage(businessAction.getMessage());
        this.setPriority(businessAction.getPriority());
        this.setState(businessAction.getState());
        this.setBusinessActionType(businessAction.getBusinessActionType());
    }

    @PersistenceConstructor
    public ProcessingBusinessAction(
            String id,
            String name,
            State state,
            int priority,
            String operationId,
            BusinessActionType businessActionType,
            GenericMessageDestination destination,
            SimpleServiceMessage message,
            String personalAccountId,
            LocalDateTime createdDate,
            LocalDateTime updatedDate,
            Map<String, Object> params
    ) {
        super(id, name, state, priority, operationId, businessActionType, destination, message);
        this.personalAccountId = personalAccountId;
        this.createdDate = createdDate;
        this.updatedDate = updatedDate;
        this.params = params;
    }

    public String getPersonalAccountId() {
        return personalAccountId;
    }

    public void setPersonalAccountId(String personalAccountId) {
        this.personalAccountId = personalAccountId;
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

    public Map<String, Object> getParams() {
        return params;
    }

    public Object getParam(String key) {
        return params.get(key);
    }

    public void addParam(String key, Object value) {
        if (params == null) {
            params = new HashMap<>();
        }

        params.put(key, value);
    }

    public void setParams(Map<String, Object> params) {
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
        return "ProcessingBusinessAction{" +
                "personalAccountId='" + personalAccountId + '\'' +
                ", createdDate=" + createdDate +
                ", updatedDate=" + updatedDate +
                ", params=" + params +
                "} " + super.toString();
    }
}
