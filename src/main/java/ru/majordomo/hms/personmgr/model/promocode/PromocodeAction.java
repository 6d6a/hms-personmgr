package ru.majordomo.hms.personmgr.model.promocode;

import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.HashMap;
import java.util.Map;

import javax.validation.constraints.NotNull;

import ru.majordomo.hms.personmgr.common.PromocodeActionType;
import ru.majordomo.hms.personmgr.model.BaseModel;

/**
 * PromocodeAction
 */
@Document
public class PromocodeAction extends BaseModel {
    @NotNull
    @Indexed
    private PromocodeActionType actionType;

    private Map<String, Object> properties = new HashMap<>();

    public PromocodeAction() {
    }

    @PersistenceConstructor
    public PromocodeAction(PromocodeActionType actionType, Map<String, Object> properties) {
        this.actionType = actionType;
        this.properties = properties;
    }

    public PromocodeActionType getActionType() {
        return actionType;
    }

    public void setActionType(PromocodeActionType actionType) {
        this.actionType = actionType;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    @Override
    public String toString() {
        return "PromocodeAction{" +
                "actionType=" + actionType +
                ", properties=" + properties +
                "} " + super.toString();
    }
}
