package ru.majordomo.hms.personmgr.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.repository.query.Param;
import ru.majordomo.hms.personmgr.common.AccountNoticeType;
import ru.majordomo.hms.personmgr.model.account.AccountNotice;

import java.util.List;

public interface AccountNoticeRepository extends MongoRepository<AccountNotice, String> {
    List<AccountNotice> findByPersonalAccountId(@Param("personalAccountId") String personalAccountId);
    List<AccountNotice> findByPersonalAccountIdAndType(
            @Param("personalAccountId") String personalAccountId,
            @Param("AccountNoticeType") AccountNoticeType type
    );
    List<AccountNotice> findByPersonalAccountIdAndViewed(
            @Param("personalAccountId") String personalAccountId,
            @Param("viewed") Boolean viewed
    );
    List<AccountNotice> findByPersonalAccountIdAndViewedAndType(
            @Param("personalAccountId") String personalAccountId,
            @Param("viewed") Boolean viewed,
            @Param("AccountNoticeType") AccountNoticeType type
    );
    AccountNotice findByPersonalAccountIdAndId(
            @Param("personalAccountId") String personalAccountId,
            @Param("id") String id
    );
}
