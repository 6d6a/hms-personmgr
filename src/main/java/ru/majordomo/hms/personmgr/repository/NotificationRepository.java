package ru.majordomo.hms.personmgr.repository;

import org.springframework.data.mongodb.repository.MongoRepository;


import ru.majordomo.hms.personmgr.common.MailManagerMessageType;
import ru.majordomo.hms.personmgr.model.notification.Notification;

import java.util.List;

public interface NotificationRepository extends MongoRepository<Notification, String> {
    Notification findByType(MailManagerMessageType type);
    Notification findByTypeAndActive(MailManagerMessageType type, boolean active);
    List<Notification> findByActive(boolean active);
}