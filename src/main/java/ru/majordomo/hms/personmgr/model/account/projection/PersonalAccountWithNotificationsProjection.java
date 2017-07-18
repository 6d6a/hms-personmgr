package ru.majordomo.hms.personmgr.model.account.projection;

import org.springframework.data.annotation.Id;

import java.util.HashSet;
import java.util.Set;

import ru.majordomo.hms.personmgr.common.MailManagerMessageType;

public class PersonalAccountWithNotificationsProjection {
    @Id
    private String id;

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
}
