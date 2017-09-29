package ru.majordomo.hms.personmgr.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.repository.query.Param;

import ru.majordomo.hms.personmgr.common.MailManagerMessageType;
import ru.majordomo.hms.personmgr.model.notification.Notification;

import java.util.List;

public interface NotificationRepository extends MongoRepository<Notification, String> {
    Notification findByType(@Param("type") MailManagerMessageType type);
    Notification findByTypeAndActive(@Param("type") MailManagerMessageType type, @Param("active") boolean active);
    List<Notification> findByActive(@Param("active") boolean active);
}