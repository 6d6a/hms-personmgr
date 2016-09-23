package ru.majordomo.hms.personmgr.model.notification;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ru.majordomo.hms.personmgr.common.MailManagerMessageType;

/**
 * AccountNotification
 */
@Document
public class AccountNotification {
    @Id
    private String id;

    @Indexed(unique = true)
    private String accountId;

    private Set<MailManagerMessageType> notifications = new HashSet<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public Set<MailManagerMessageType> getNotifications() {
        return notifications;
    }

    public void setNotifications(Set<MailManagerMessageType> notifications) {
        this.notifications = notifications;
    }

    public boolean hasNotification(MailManagerMessageType notification) {
        return this.notifications.contains(notification);
    }

    public void addNotification(MailManagerMessageType notification) {
        this.notifications.add(notification);
    }

    public void addNotifications(Set<MailManagerMessageType> notifications) {
        this.notifications.addAll(notifications);
    }

    public void removeNotification(MailManagerMessageType notification) {
        this.notifications.remove(notification);
    }

    public AccountNotification() {
    }

    public AccountNotification(String id, String accountId, Set<MailManagerMessageType> notifications) {
        this.id = id;
        this.accountId = accountId;
        this.notifications = notifications;
    }

    public AccountNotification(String accountId, Set<MailManagerMessageType> notifications) {
        this.accountId = accountId;
        this.notifications = notifications;
    }

    @Override
    public String toString() {
        return "AccountNotification{" +
                "id='" + id + '\'' +
                ", accountId='" + accountId + '\'' +
                ", notifications=" + notifications +
                '}';
    }
}
