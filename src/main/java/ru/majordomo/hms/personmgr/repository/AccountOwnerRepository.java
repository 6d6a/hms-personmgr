package ru.majordomo.hms.personmgr.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;


import java.util.List;

import ru.majordomo.hms.personmgr.model.account.AccountOwner;

public interface AccountOwnerRepository extends MongoRepository<AccountOwner, String>,
        QuerydslPredicateExecutor<AccountOwner> {
    AccountOwner findOneByPersonalAccountId(String personalAccountId);
    List<AccountOwner> findByPersonalAccountId(String personalAccountId);
    Page<AccountOwner> findByPersonalAccountId(String personalAccountId, Pageable pageable);
    AccountOwner findByIdAndPersonalAccountId(String id, String personalAccountId);
    List<AccountOwner> findAllByTypeIn(List<AccountOwner.Type> types);
    void deleteOneByPersonalAccountId(String personalAccountId);
}