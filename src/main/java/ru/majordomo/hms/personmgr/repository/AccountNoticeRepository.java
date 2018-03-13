package ru.majordomo.hms.personmgr.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.repository.query.Param;
import ru.majordomo.hms.personmgr.model.account.AccountNotice;

import java.util.List;

public interface AccountNoticeRepository extends MongoRepository<AccountNotice, String> {
    List<AccountNotice> findByPersonalAccountId(@Param("personalAccountId") String personalAccountId);
    List<AccountNotice> findByPersonalAccountIdAndViewed(
            @Param("personalAccountId") String personalAccountId,
            @Param("viewed") Boolean viewed
    );
    AccountNotice findByPersonalAccountIdAndId(
            @Param("personalAccountId") String personalAccountId,
            @Param("id") String id
    );
}
