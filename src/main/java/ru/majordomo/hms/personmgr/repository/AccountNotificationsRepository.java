package ru.majordomo.hms.personmgr.repository;


import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

import ru.majordomo.hms.personmgr.model.notification.AccountNotification;

public interface AccountNotificationsRepository extends MongoRepository<AccountNotification, String> {
    AccountNotification findOne(String id);
    List<AccountNotification> findAll();
    AccountNotification findByAccountId(@Param("accountId") String accountId);
}