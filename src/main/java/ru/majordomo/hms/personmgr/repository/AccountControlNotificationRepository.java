package ru.majordomo.hms.personmgr.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.repository.query.Param;
import ru.majordomo.hms.personmgr.model.account.AccountControlNotification;

import java.util.List;

public interface AccountControlNotificationRepository extends MongoRepository<AccountControlNotification, String> {
    List<AccountControlNotification> findByPersonalAccountId(@Param("personalAccountId") String personalAccountId);
    List<AccountControlNotification> findByPersonalAccountIdAndViewed(
            @Param("personalAccountId") String personalAccountId,
            @Param("viewed") Boolean viewed
    );
    AccountControlNotification findByPersonalAccountIdAndId(
            @Param("personalAccountId") String personalAccountId,
            @Param("id") String id
    );
}
