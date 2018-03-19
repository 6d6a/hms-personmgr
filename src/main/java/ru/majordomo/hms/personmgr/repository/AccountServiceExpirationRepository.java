package ru.majordomo.hms.personmgr.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.repository.query.Param;
import ru.majordomo.hms.personmgr.model.service.AccountServiceExpiration;

import java.util.List;

public interface AccountServiceExpirationRepository extends MongoRepository<AccountServiceExpiration, String> {
    List<AccountServiceExpiration> findByPersonalAccountId(@Param("personalAccountId") String personalAccountId);
    AccountServiceExpiration findByPersonalAccountIdAndAccountServiceId(
            @Param("personalAccountId") String personalAccountId,
            @Param("accountServiceId") String accountServiceId
    );
}
