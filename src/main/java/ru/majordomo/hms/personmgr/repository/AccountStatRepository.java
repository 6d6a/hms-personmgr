package ru.majordomo.hms.personmgr.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;


import java.time.LocalDateTime;
import java.util.List;

import ru.majordomo.hms.personmgr.common.AccountStatType;
import ru.majordomo.hms.personmgr.model.account.AccountStat;

public interface AccountStatRepository extends MongoRepository<AccountStat, String> {
    List<AccountStat> findByPersonalAccountId(String personalAccountId);
    List<AccountStat> findByPersonalAccountIdAndType(String personalAccountId, AccountStatType type);
    Page<AccountStat> findByPersonalAccountId(String personalAccountId, Pageable pageable);
    AccountStat findByIdAndPersonalAccountId(String id, String personalAccountId);
    List<AccountStat> findByPersonalAccountIdAndTypeAndCreatedAfterOrderByCreatedDesc(
            String personalAccountId,
            AccountStatType type,
            LocalDateTime created
    );

    AccountStat findFirstByPersonalAccountIdAndTypeAndCreatedAfterOrderByCreatedDesc(
            String personalAccountId,
            AccountStatType type,
            LocalDateTime created
    );

    AccountStat findFirstByPersonalAccountIdAndTypeInAndCreatedAfterOrderByCreatedDesc(
            String personalAccountId,
            List<AccountStatType> types,
            LocalDateTime created
    );

    AccountStat findFirstByPersonalAccountIdAndTypeOrderByCreatedDesc(
            String personalAccountId,
            AccountStatType type
    );

    boolean existsByPersonalAccountIdAndType(
            String personalAccountId,
            AccountStatType type
    );

    Integer countAccountStatByTypeAndCreatedIsBetween(
            AccountStatType type,
            LocalDateTime after,
            LocalDateTime before
    );

    List<AccountStat> findByPersonalAccountIdInAndType(
            List<String> personalAccountIds,
            AccountStatType type
    );

    Integer countAccountStatByPersonalAccountIdAndTypeAndCreatedIsBetween(
            String personalAccountId,
            AccountStatType type,
            LocalDateTime after,
            LocalDateTime before
    );

    List<AccountStat> findByTypeAndCreatedAfterOrderByCreatedDesc(
            AccountStatType type,
            LocalDateTime created
    );
}