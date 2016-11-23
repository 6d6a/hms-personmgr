package ru.majordomo.hms.personmgr.repository;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

import ru.majordomo.hms.personmgr.model.AccountHistory;

public interface AccountHistoryRepository extends MongoRepository<AccountHistory, String> {
    AccountHistory findOne(String id);
    List<AccountHistory> findAll();
    List<AccountHistory> findByPersonalAccountId(@Param("personalAccountId") String personalAccountId);
    Page<AccountHistory> findByPersonalAccountId(@Param("personalAccountId") String personalAccountId, Pageable pageable);
    AccountHistory findByIdAndPersonalAccountId(@Param("id") String id, @Param("personalAccountId") String personalAccountId);
}