package ru.majordomo.hms.personmgr.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

import java.util.List;

import ru.majordomo.hms.personmgr.model.account.AccountHistory;

public interface AccountHistoryRepository extends MongoRepository<AccountHistory, String>,
        QuerydslPredicateExecutor<AccountHistory> {
    List<AccountHistory> findByPersonalAccountId(String personalAccountId);
    Page<AccountHistory> findByPersonalAccountId(String personalAccountId, Pageable pageable);
    AccountHistory findByIdAndPersonalAccountId(String id, String personalAccountId);
    void deleteByPersonalAccountId(String personalAccountId);
}