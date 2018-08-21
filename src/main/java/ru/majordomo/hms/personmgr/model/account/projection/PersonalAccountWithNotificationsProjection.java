package ru.majordomo.hms.personmgr.model.account.projection;

import com.fasterxml.jackson.annotation.JsonFormat;

import org.springframework.data.annotation.Id;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import lombok.Data;
import ru.majordomo.hms.personmgr.common.MailManagerMessageType;

@Data
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

    private boolean active = true;

    public boolean hasNotification(MailManagerMessageType notification) {
        return this.notifications.contains(notification);
    }
}
