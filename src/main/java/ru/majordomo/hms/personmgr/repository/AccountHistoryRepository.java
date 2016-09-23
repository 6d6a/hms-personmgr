package ru.majordomo.hms.personmgr.repository;


import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

import ru.majordomo.hms.personmgr.model.AccountHistory;

public interface AccountHistoryRepository extends MongoRepository<AccountHistory, String> {
    AccountHistory findOne(String id);
    List<AccountHistory> findAll();
    List<AccountHistory> findByAccountId(@Param("accountId") String accountId);
}