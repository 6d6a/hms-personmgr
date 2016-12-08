package ru.majordomo.hms.personmgr.repository;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RestResource;

import java.util.List;

import ru.majordomo.hms.personmgr.model.service.AccountService;

public interface AccountServiceRepository extends MongoRepository<AccountService,String> {
    AccountService findOne(String id);
    List<AccountService> findAll();
    AccountService findByPersonalAccountIdAndId(@Param("personalAccountId") String personalAccountId, @Param("id") String id);
    @RestResource(path = "findListByPersonalAccountId", rel = "findListByPersonalAccountId")
    List<AccountService> findByPersonalAccountId(@Param("personalAccountId") String personalAccountId);
    Page<AccountService> findByPersonalAccountId(@Param("personalAccountId") String personalAccountId, Pageable pageable);
    AccountService findByPersonalAccountIdAndServiceId(@Param("personalAccountId") String personalAccountId, @Param("serviceId") String serviceId);
}