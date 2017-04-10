package ru.majordomo.hms.personmgr.repository;

import feign.Param;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

import ru.majordomo.hms.personmgr.model.present.AccountPresent;

public interface AccountPresentRepository extends MongoRepository<AccountPresent, String> {
    AccountPresent findOne(String id);
    List<AccountPresent> findAll();
    List<AccountPresent> findByPersonalAccountId(@Param("personalAccountId") String personalAccountId);
    List<AccountPresent> findByPersonalAccountIdAndPresentId(@Param("personalAccountId") String personalAccountId, @Param("presentId") String presentId);
    Long countByPersonalAccountIdAndPresentId(@Param("personalAccountId") String personalAccountId, @Param("presentId") String presentId);
}
