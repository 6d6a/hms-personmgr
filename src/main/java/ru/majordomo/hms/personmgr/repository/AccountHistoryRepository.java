package ru.majordomo.hms.personmgr.repository;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.querydsl.QueryDslPredicateExecutor;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RestResource;

import java.util.List;

import ru.majordomo.hms.personmgr.model.AccountHistory;

public interface AccountHistoryRepository extends MongoRepository<AccountHistory, String>,
        QueryDslPredicateExecutor<AccountHistory> {
    AccountHistory findOne(String id);
    List<AccountHistory> findAll();
    @RestResource(path = "findListByPersonalAccountId", rel = "findListByPersonalAccountId")
    List<AccountHistory> findByPersonalAccountId(@Param("personalAccountId") String personalAccountId);
    Page<AccountHistory> findByPersonalAccountId(@Param("personalAccountId") String personalAccountId, Pageable pageable);
    AccountHistory findByIdAndPersonalAccountId(@Param("id") String id, @Param("personalAccountId") String personalAccountId);
}