package ru.majordomo.hms.personmgr.model;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import ru.majordomo.hms.personmgr.common.State;

public class ProcessingBusinessOperation extends Step  {

    private String accountName;

    @Indexed
    private String personalAccountId;

    @CreatedDate
    private LocalDateTime createdDate;

    @LastModifiedDate
    private LocalDateTime updatedDate;

    @JsonIgnore
    private Map<String,Object> mapParams = new HashMap<>();

    public ProcessingBusinessOperation() {
    }

    @PersistenceConstructor
    public ProcessingBusinessOperation(String id, String name, State state, int priority, String accountName, String personalAccountId, LocalDateTime createdDate, LocalDateTime updatedDate, Map<String, Object> mapParams) {
        super();
        this.setId(id);
        this.setName(name);
        this.setState(state);
        this.setPriority(priority);
        this.accountName = accountName;
        this.personalAccountId = personalAccountId;
        this.createdDate = createdDate;
        this.updatedDate = updatedDate;
        this.mapParams = mapParams;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
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

    public Map<String, Object> getMapParams() {
        return mapParams;
    }

    public void setMapParams(Map<String, Object> mapParams) {
        this.mapParams = mapParams;
    }

    public void addMapParam(String key, Object value) {
        this.mapParams.put(key, value);
    }

    public Object getMapParam(String key) {
        return this.mapParams.get(key);
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
                "accountName='" + accountName + '\'' +
                ", personalAccountId='" + personalAccountId + '\'' +
                ", createdDate=" + createdDate +
                ", updatedDate=" + updatedDate +
                ", mapParams=" + mapParams +
                "} " + super.toString();
    }
}
