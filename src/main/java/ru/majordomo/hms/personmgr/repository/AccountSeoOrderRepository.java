package ru.majordomo.hms.personmgr.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

import ru.majordomo.hms.personmgr.model.seo.AccountSeoOrder;

public interface AccountSeoOrderRepository extends MongoRepository<AccountSeoOrder, String> {
    List<AccountSeoOrder> findBySeoId(@Param("seoId") String seoId);
    List<AccountSeoOrder> findByDomainName(@Param("domainName") String domainName);
    List<AccountSeoOrder> findByPersonalAccountId(@Param("personalAccountId") String personalAccountId);
    AccountSeoOrder findByPersonalAccountIdAndDomainNameAndCreatedAfter(
            @Param("personalAccountId") String personalAccountId,
            @Param("domainName") String domainName,
            @Param("created") LocalDateTime created
    );
}