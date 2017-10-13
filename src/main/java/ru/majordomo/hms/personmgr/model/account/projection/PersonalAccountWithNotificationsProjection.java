package ru.majordomo.hms.personmgr.model.account.projection;

import com.fasterxml.jackson.annotation.JsonFormat;

import org.springframework.data.annotation.Id;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import ru.majordomo.hms.personmgr.common.MailManagerMessageType;

public class PersonalAccountWithNotificationsProjection {
    @Id
    private String id;

    private String accountId;

    @JsonFormat
            (shape = JsonFormat.Shape.STRING,
             pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime deleted;

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

    public LocalDateTime getDeleted() {
        return deleted;
    }

    public void setDeleted(LocalDateTime deleted) {
        this.deleted = deleted;
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
