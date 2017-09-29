package ru.majordomo.hms.personmgr.model.notification;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import ru.majordomo.hms.personmgr.common.MailManagerMessageType;
import ru.majordomo.hms.personmgr.common.Views;
import ru.majordomo.hms.personmgr.model.BaseModel;

@Document
public class Notification extends BaseModel {
    @Indexed(unique = true)
    @JsonView(Views.Public.class)
    private MailManagerMessageType type;

    @JsonView(Views.Public.class)
    private String name;

    @JsonView(Views.Internal.class)
    private String apiName;

    @JsonIgnore
    private boolean active = true;

    public Notification() {
    }

    public Notification(String id, MailManagerMessageType type, String name, String apiName) {
        this.setId(id);
        this.type = type;
        this.name = name;
        this.apiName = apiName;
    }

    public Notification(MailManagerMessageType type, String name, String apiName) {
        this.type = type;
        this.name = name;
        this.apiName = apiName;
    }

    public MailManagerMessageType getType() {
        return type;
    }

    public void setType(MailManagerMessageType type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getApiName() {
        return apiName;
    }

    public void setApiName(String apiName) {
        this.apiName = apiName;
    }

    @Override
    public String toString() {
        return "Notification{" +
                "type=" + type +
                ", name='" + name + '\'' +
                ", apiName='" + apiName + '\'' +
                "} " + super.toString();
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
