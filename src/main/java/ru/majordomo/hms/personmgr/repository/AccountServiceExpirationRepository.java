package ru.majordomo.hms.personmgr.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import ru.majordomo.hms.personmgr.model.service.AccountServiceExpiration;

import java.util.List;
import java.util.stream.Stream;

public interface AccountServiceExpirationRepository extends MongoRepository<AccountServiceExpiration, String> {
    List<AccountServiceExpiration> findByPersonalAccountId(String personalAccountId);
    AccountServiceExpiration findByPersonalAccountIdAndAccountServiceId(
            String personalAccountId,
            String accountServiceId
    );
    @Query("{}")
    Stream<AccountServiceExpiration> findAllStream();
}
