package ru.majordomo.hms.personmgr.repository;


import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

import ru.majordomo.hms.personmgr.model.AccountHistory;
import ru.majordomo.hms.personmgr.model.notification.AccountNotifications;

public interface AccountNotificationsRepository extends MongoRepository<AccountNotifications, String> {
    AccountNotifications findOne(String id);
    List<AccountNotifications> findAll();
    AccountNotifications findByAccountId(@Param("accountId") String accountId);
}