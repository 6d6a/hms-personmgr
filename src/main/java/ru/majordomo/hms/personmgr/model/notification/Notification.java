package ru.majordomo.hms.personmgr.model.notification;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import ru.majordomo.hms.personmgr.common.MailManagerMessageType;

/**
 * Notification
 */
@Document
public class Notification {
    @Id
    private String id;

    @Indexed(unique = true)
    private MailManagerMessageType type;

    private String name;

    private String apiName;

    public Notification() {
    }

    public Notification(String id, MailManagerMessageType type, String name, String apiName) {
        this.id = id;
        this.type = type;
        this.name = name;
        this.apiName = apiName;
    }

    public Notification(MailManagerMessageType type, String name, String apiName) {
        this.type = type;
        this.name = name;
        this.apiName = apiName;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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
                "id='" + id + '\'' +
                ", type=" + type +
                ", name='" + name + '\'' +
                ", apiName='" + apiName + '\'' +
                '}';
    }
}
