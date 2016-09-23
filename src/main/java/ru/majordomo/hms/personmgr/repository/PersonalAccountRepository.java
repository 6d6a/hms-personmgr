package ru.majordomo.hms.personmgr.repository;


import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.repository.query.Param;

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
    @Query("{}")
    Stream<PersonalAccount> findAllStream();
}