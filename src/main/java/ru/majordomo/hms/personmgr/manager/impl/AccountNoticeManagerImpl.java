package ru.majordomo.hms.personmgr.manager.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;
import ru.majordomo.hms.personmgr.common.AccountNoticeType;
import ru.majordomo.hms.personmgr.manager.AccountNoticeManager;
import ru.majordomo.hms.personmgr.model.account.AccountNotice;
import ru.majordomo.hms.personmgr.model.account.DeferredPlanChangeNotice;
import ru.majordomo.hms.personmgr.repository.AccountNoticeRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AccountNoticeManagerImpl implements AccountNoticeManager {
    private final AccountNoticeRepository accountNoticeRepository;
    private final MongoOperations mongoOperations;

    @Override
    public List<AccountNotice> findByPersonalAccountId(String personalAccountId) {
        return accountNoticeRepository.findByPersonalAccountId(personalAccountId);
    }

    @Override
    public List<AccountNotice> findByPersonalAccountIdAndType(String personalAccountId, AccountNoticeType type) {
        return accountNoticeRepository.findByPersonalAccountIdAndType(personalAccountId, type);
    }

    @Override
    public List<AccountNotice> findByPersonalAccountIdAndViewed(String personalAccountId, Boolean viewed) {
        return accountNoticeRepository.findByPersonalAccountIdAndViewed(personalAccountId, viewed);
    }

    @Override
    public List<AccountNotice> findByPersonalAccountIdAndViewedAndType(String personalAccountId, Boolean viewed, AccountNoticeType type) {
        return accountNoticeRepository.findByPersonalAccountIdAndViewedAndType(personalAccountId, viewed, type);
    }

    @Override
    public AccountNotice findByPersonalAccountIdAndId(String personalAccountId, String id) {
        return accountNoticeRepository.findByPersonalAccountIdAndId(personalAccountId, id);
    }

    @Override
    public boolean existsByPersonalAccountIdAndTypeAndViewed(String personalAccountId, AccountNoticeType type, Boolean viewed) {
        return accountNoticeRepository.existsByPersonalAccountIdAndTypeAndViewed(personalAccountId, type, viewed);
    }

    @Override
    public Optional<AccountNotice> findById(String id) {
        return accountNoticeRepository.findById(id);
    }

    public void save(AccountNotice accountNotice) {
        mongoOperations.save(accountNotice, AccountNotice.COLLECTION_NAME); // так как сохранение через accountNoticeRepository перестало работать после обновления Spring
    }

    @Override
    public AccountNotice insert(AccountNotice accountNotice) {
        return mongoOperations.insert(accountNotice, AccountNotice.COLLECTION_NAME); // так как сохранение через accountNoticeRepository перестало работать после обновления Spring
    }

    @Override
    public List<DeferredPlanChangeNotice> findDeferredPlanChangeNoticeByWasChanged(boolean wasChanged) {
        Query query = new Query(new Criteria("wasChanged").is(wasChanged).and("_class").is(DeferredPlanChangeNotice.class.getName()));
        return mongoOperations.find(query, DeferredPlanChangeNotice.class, AccountNotice.COLLECTION_NAME);
    }

    @Override
    public List<DeferredPlanChangeNotice> findDeferredPlanChangeNoticeByWasChangedAndWillBeChangedAfterLessThan(boolean wasChanged, LocalDate willBeChangedAfter) {
        Query query = new Query(new Criteria("wasChanged").is(wasChanged).and("_class").is(DeferredPlanChangeNotice.class.getName()).and("willBeChangedAfter").lt(willBeChangedAfter));
        return mongoOperations.find(query, DeferredPlanChangeNotice.class, AccountNotice.COLLECTION_NAME);
    }
}
