package ru.majordomo.hms.personmgr.repository;


import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

import ru.majordomo.hms.personmgr.model.domain.AccountDomain;

public interface AccountDomainRepository extends MongoRepository<AccountDomain, String> {
    AccountDomain findOne(String id);
    List<AccountDomain> findAll();
    List<AccountDomain> findByPersonalAccountId(@Param("personalAccountId") String personalAccountId);
}