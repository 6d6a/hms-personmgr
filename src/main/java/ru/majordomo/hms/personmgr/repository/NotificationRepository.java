package ru.majordomo.hms.personmgr.repository;


import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

import ru.majordomo.hms.personmgr.common.MailManagerMessageType;
import ru.majordomo.hms.personmgr.model.notification.AccountNotifications;
import ru.majordomo.hms.personmgr.model.notification.Notification;

public interface NotificationRepository extends MongoRepository<Notification, String> {
    Notification findOne(String id);
    List<Notification> findAll();
    Notification findByType(@Param("type") MailManagerMessageType type);
}