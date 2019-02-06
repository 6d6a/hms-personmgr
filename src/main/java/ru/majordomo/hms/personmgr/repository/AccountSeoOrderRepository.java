package ru.majordomo.hms.personmgr.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.List;

import ru.majordomo.hms.personmgr.model.seo.AccountSeoOrder;

public interface AccountSeoOrderRepository extends MongoRepository<AccountSeoOrder, String> {
    List<AccountSeoOrder> findBySeoId(String seoId);
    List<AccountSeoOrder> findByDomainName(String domainName);
    List<AccountSeoOrder> findByPersonalAccountId(String personalAccountId);
    List<AccountSeoOrder> findByPersonalAccountIdAndDomainNameAndCreatedAfter(
            String personalAccountId,
            String domainName,
            LocalDateTime created
    );
}