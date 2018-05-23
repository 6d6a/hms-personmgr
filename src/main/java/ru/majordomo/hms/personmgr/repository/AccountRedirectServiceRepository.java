package ru.majordomo.hms.personmgr.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import ru.majordomo.hms.personmgr.model.service.RedirectAccountService;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

public interface AccountRedirectServiceRepository extends MongoRepository<RedirectAccountService, String> {
    RedirectAccountService findByPersonalAccountIdAndFullDomainName(String personalAccountId, String fullDomainName);
    List<RedirectAccountService> findByPersonalAccountId(String personalAccountId);
    boolean existsByPersonalAccountIdAndFullDomainNameAndExpireDateAfter(String personalAccountId, String fullDomainName, LocalDate date);
    RedirectAccountService findByPersonalAccountIdAndId(String accountId, String serviceId);
    Stream<RedirectAccountService> findByPersonalAccountIdInAndExpireDateAfter(List<String> personalAccountIds, LocalDate date);
}
