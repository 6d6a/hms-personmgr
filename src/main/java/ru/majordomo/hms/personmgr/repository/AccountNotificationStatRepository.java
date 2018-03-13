package ru.majordomo.hms.personmgr.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.repository.query.Param;
import ru.majordomo.hms.personmgr.common.AccountType;
import ru.majordomo.hms.personmgr.common.NotificationTransportType;
import ru.majordomo.hms.personmgr.common.NotificationType;
import ru.majordomo.hms.personmgr.model.account.AccountNotificationStat;

import java.time.LocalDateTime;
import java.util.List;

public interface AccountNotificationStatRepository extends MongoRepository<AccountNotificationStat, String>{
    boolean existsByAccountTypeAndTransportTypeInAndPersonalAccountIdAndCreatedAfterAndNotificationType(
            @Param("accountType") AccountType accountType,
            @Param("transportType") List<NotificationTransportType> transportTypes,
            @Param("personalAccountid") String personalAccountId,
            @Param("created") LocalDateTime created,
            @Param("notificationType") NotificationType notificationType
    );
}
