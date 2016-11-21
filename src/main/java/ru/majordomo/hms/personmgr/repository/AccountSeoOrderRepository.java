package ru.majordomo.hms.personmgr.repository;


import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

import ru.majordomo.hms.personmgr.model.seo.AccountSeoOrder;

public interface AccountSeoOrderRepository extends MongoRepository<AccountSeoOrder, String> {
    AccountSeoOrder findOne(String id);
    List<AccountSeoOrder> findAll();
    List<AccountSeoOrder> findBySeoId(@Param("seoId") String seoId);
    List<AccountSeoOrder> findByWebSiteId(@Param("webSiteId") String webSiteId);
    List<AccountSeoOrder> findByPersonalAccountId(@Param("personalAccountId") String personalAccountId);
    AccountSeoOrder findByPersonalAccountIdAndWebSiteIdAndCreatedAfter(@Param("personalAccountId") String personalAccountId, @Param("webSiteId") String webSiteId, @Param("created") LocalDateTime created);
}