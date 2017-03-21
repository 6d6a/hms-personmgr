package ru.majordomo.hms.personmgr.repository;

import feign.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.rest.core.annotation.RestResource;
import ru.majordomo.hms.personmgr.model.abonement.AccountAbonementPreorder;

import java.util.List;

public interface AccountAbonementPreorderRepository extends MongoRepository<AccountAbonementPreorder, String> {
    AccountAbonementPreorder findOne(String id);
    AccountAbonementPreorder findByIdAndPersonalAccountId(@Param("id") String id, @Param("personalAccountId") String personalAccountId);
    List<AccountAbonementPreorder> findAll();
    @RestResource(path = "findListByPersonalAccountId", rel = "findListByPersonalAccountId")
    AccountAbonementPreorder findByPersonalAccountId(@Param("personalAccountId") String personalAccountId);
}
