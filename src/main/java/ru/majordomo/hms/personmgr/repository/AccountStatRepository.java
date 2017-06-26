package ru.majordomo.hms.personmgr.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RestResource;

import java.time.LocalDateTime;
import java.util.List;

import ru.majordomo.hms.personmgr.common.AccountStatType;
import ru.majordomo.hms.personmgr.model.account.AccountStat;

public interface AccountStatRepository extends MongoRepository<AccountStat, String> {
    @RestResource(path = "findListByPersonalAccountId", rel = "findListByPersonalAccountId")
    List<AccountStat> findByPersonalAccountId(@Param("personalAccountId") String personalAccountId);
    List<AccountStat> findByPersonalAccountIdAndType(@Param("personalAccountId") String personalAccountId, @Param("type") AccountStatType type);
    Page<AccountStat> findByPersonalAccountId(@Param("personalAccountId") String personalAccountId, Pageable pageable);
    AccountStat findByIdAndPersonalAccountId(@Param("id") String id, @Param("personalAccountId") String personalAccountId);
    List<AccountStat> findByType(@Param("type") AccountStatType type);
    List<AccountStat> findByPersonalAccountIdAndTypeAndCreatedAfterOrderByCreatedDesc(
            @Param("personalAccountId") String personalAccountId,
            @Param("type") AccountStatType type,
            @Param("created") LocalDateTime created
    );
}