package ru.majordomo.hms.personmgr.repository;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RestResource;

import java.util.List;
import java.util.stream.Stream;

import ru.majordomo.hms.personmgr.common.AccountType;
import ru.majordomo.hms.personmgr.model.PersonalAccount;

public interface PersonalAccountRepository extends MongoRepository<PersonalAccount, String> {
    PersonalAccount findOne(String id);
    List<PersonalAccount> findAll();
    PersonalAccount findByName(@Param("name") String name);
    PersonalAccount findByClientId(@Param("clientId") String clientId);
    PersonalAccount findByAccountId(@Param("accountId") String accountId);
    List<PersonalAccount> findByAccountType(@Param("accountType") AccountType accountType);

    @RestResource(path = "findListByActive", rel = "findListByActive")
    List<PersonalAccount> findByActive(@Param("active") boolean active);
    Page<PersonalAccount> findByActive(@Param("active") boolean active, Pageable pageable);
    @Query("{}")
    Stream<PersonalAccount> findAllStream();
}