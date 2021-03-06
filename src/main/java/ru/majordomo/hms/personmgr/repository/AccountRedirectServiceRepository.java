package ru.majordomo.hms.personmgr.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import ru.majordomo.hms.personmgr.model.service.RedirectAccountService;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

public interface AccountRedirectServiceRepository extends MongoRepository<RedirectAccountService, String> {
    RedirectAccountService findByPersonalAccountIdAndFullDomainName(String personalAccountId, String fullDomainName);
    List<RedirectAccountService> findByPersonalAccountId(String personalAccountId);
    RedirectAccountService findByPersonalAccountIdAndId(String accountId, String serviceId);
    RedirectAccountService findByAccountServiceAbonementId(String accountServiceAbonementId);
    @Query("{}")
    Stream<RedirectAccountService> findAllStream();
}
