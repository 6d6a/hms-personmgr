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

    AccountStat findFirstByPersonalAccountIdAndTypeAndCreatedAfterOrderByCreatedDesc(
            @Param("personalAccountId") String personalAccountId,
            @Param("type") AccountStatType type,
            @Param("created") LocalDateTime created
    );

    AccountStat findFirstByPersonalAccountIdAndTypeInAndCreatedAfterOrderByCreatedDesc(
            @Param("personalAccountId") String personalAccountId,
            @Param("type") List<AccountStatType> types,
            @Param("created") LocalDateTime created
    );

    boolean existsByPersonalAccountIdAndType(
            @Param("personalAccountId") String personalAccountId,
            @Param("type") AccountStatType type
    );

    Integer countAccountStatByTypeAndCreatedIsBetween(
            @Param("type") AccountStatType type,
            @Param("after") LocalDateTime after,
            @Param("before") LocalDateTime before
    );

    List<AccountStat> findByPersonalAccountIdInAndType(
            @Param("personalAccountIds") List<String> personalAccountIds,
            @Param("type") AccountStatType type
    );

    Integer countAccountStatByPersonalAccountIdAndTypeAndCreatedIsBetween(
            @Param("personalAccountId") String personalAccountId,
            @Param("type") AccountStatType type,
            @Param("after") LocalDateTime after,
            @Param("before") LocalDateTime before
    );
}