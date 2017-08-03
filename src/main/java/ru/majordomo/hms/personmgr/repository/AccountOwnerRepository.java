package ru.majordomo.hms.personmgr.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RestResource;

import java.util.List;

import ru.majordomo.hms.personmgr.model.account.AccountOwner;

public interface AccountOwnerRepository extends MongoRepository<AccountOwner, String> {
    AccountOwner findOneByPersonalAccountId(@Param("personalAccountId") String personalAccountId);
    @RestResource(path = "findListByPersonalAccountId", rel = "findListByPersonalAccountId")
    List<AccountOwner> findByPersonalAccountId(@Param("personalAccountId") String personalAccountId);
    Page<AccountOwner> findByPersonalAccountId(@Param("personalAccountId") String personalAccountId, Pageable pageable);
    AccountOwner findByIdAndPersonalAccountId(@Param("id") String id, @Param("personalAccountId") String personalAccountId);
    List<AccountOwner> findAllByTypeIn(List<AccountOwner.Type> types);
    void deleteOneByPersonalAccountId(@Param("personalAccountId") String personalAccountId);
}