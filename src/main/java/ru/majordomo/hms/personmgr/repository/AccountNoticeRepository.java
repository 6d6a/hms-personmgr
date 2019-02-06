package ru.majordomo.hms.personmgr.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import ru.majordomo.hms.personmgr.common.AccountNoticeType;
import ru.majordomo.hms.personmgr.model.account.AccountNotice;

import java.util.List;

public interface AccountNoticeRepository extends MongoRepository<AccountNotice, String> {
    List<AccountNotice> findByPersonalAccountId(String personalAccountId);
    List<AccountNotice> findByPersonalAccountIdAndType(
            String personalAccountId,
            AccountNoticeType type
    );
    List<AccountNotice> findByPersonalAccountIdAndViewed(
            String personalAccountId,
            Boolean viewed
    );
    List<AccountNotice> findByPersonalAccountIdAndViewedAndType(
            String personalAccountId,
            Boolean viewed,
            AccountNoticeType type
    );
    AccountNotice findByPersonalAccountIdAndId(
            String personalAccountId,
            String id
    );
    boolean existsByPersonalAccountIdAndTypeAndViewed(String personalAccountId, AccountNoticeType type, Boolean viewed);
}
