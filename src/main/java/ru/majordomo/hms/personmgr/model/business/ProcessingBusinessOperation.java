package ru.majordomo.hms.personmgr.model.business;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonView;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import ru.majordomo.hms.personmgr.common.BusinessOperationType;
import ru.majordomo.hms.personmgr.common.State;
import ru.majordomo.hms.personmgr.common.Views;

@Document
public class ProcessingBusinessOperation extends Step {
    @JsonView(Views.Public.class)
    @Indexed
    private BusinessOperationType type = BusinessOperationType.COMMON_OPERATION;

    @JsonView(Views.Public.class)
    @Indexed
    private String personalAccountId;

    @JsonView(Views.Public.class)
    @CreatedDate
    @Indexed
    @JsonFormat
            (shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdDate;

    @JsonView(Views.Public.class)
    @LastModifiedDate
    @Indexed
    @JsonFormat
            (shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedDate;

    @JsonView(Views.Internal.class)
    private Map<String,Object> params = new HashMap<>();

    @JsonView(Views.Public.class)
    private Map<String,Object> publicParams = new HashMap<>();

    public ProcessingBusinessOperation() {
    }

    @PersistenceConstructor
    public ProcessingBusinessOperation(
            String id,
            String name,
            State state,
            int priority,
            BusinessOperationType type,
            String personalAccountId,
            LocalDateTime createdDate,
            LocalDateTime updatedDate,
            Map<String, Object> params
    ) {
        super();
        this.setId(id);
        this.setName(name);
        this.setState(state);
        this.setPriority(priority);
        this.type = type;
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

    public void setParams(Map<String, Object> params) {
        this.params = params;
    }

    public void addParam(String key, Object value) {
        if (params == null) {
            params = new HashMap<>();
        }

        params.put(key, value);
    }

    public Object getParam(String key) {
        return this.params.get(key);
    }

    public Map<String, Object> getPublicParams() {
        return publicParams;
    }

    public void setPublicParams(Map<String, Object> publicParams) {
        this.publicParams = publicParams;
    }

    public void addPublicParam(String key, Object value) {
        if (publicParams == null) {
            publicParams = new HashMap<>();
        }

        publicParams.put(key, value);
    }

    public Object getPublicParam(String key) {
        return this.publicParams.get(key);
    }

    public BusinessOperationType getType() {
        return type;
    }

    public void setType(BusinessOperationType type) {
        this.type = type;
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
        return "ProcessingBusinessOperation{" +
                ", type=" + type +
                ", personalAccountId='" + personalAccountId + '\'' +
                ", createdDate=" + createdDate +
                ", updatedDate=" + updatedDate +
                ", params=" + params +
                "} " + super.toString();
    }
}
