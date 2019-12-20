package ru.majordomo.hms.personmgr.manager;

import org.springframework.data.domain.Example;
import ru.majordomo.hms.personmgr.common.AccountNoticeType;
import ru.majordomo.hms.personmgr.model.account.AccountNotice;
import ru.majordomo.hms.personmgr.model.account.DeferredPlanChangeNotice;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AccountNoticeManager {
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

    void save(AccountNotice accountNotice);

    AccountNotice insert(AccountNotice accountNotice);

    Optional<AccountNotice> findById(String id);

    List<DeferredPlanChangeNotice> findDeferredPlanChangeNoticeByWasChanged(boolean wasChanged);

    List<DeferredPlanChangeNotice> findDeferredPlanChangeNoticeByWasChangedAndWillBeChangedAfterLessThan(boolean wasChanged, LocalDate willBeChanged);
}
