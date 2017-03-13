package ru.majordomo.hms.personmgr.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.querydsl.QueryDslPredicateExecutor;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RestResource;

import java.util.List;

import ru.majordomo.hms.personmgr.model.promocode.AccountPromocode;

public interface AccountPromocodeRepository extends MongoRepository<AccountPromocode, String>,
        QueryDslPredicateExecutor<AccountPromocode> {
    AccountPromocode findOne(String id);
    List<AccountPromocode> findAll();
    List<AccountPromocode> findByPromocodeId(@Param("promocodeId") String promocodeId);
    AccountPromocode findOneByPromocodeId(@Param("promocodeId") String promocodeId);
    AccountPromocode findByPromocodeIdAndOwnedByAccount(@Param("promocodeId") String promocodeId, @Param("ownedByAccount") boolean ownedByAccount);
    List<AccountPromocode> findByPersonalAccountIdAndOwnedByAccount(@Param("personalAccountId") String personalAccountId, @Param("ownedByAccount") boolean ownedByAccount);
    List<AccountPromocode> findByOwnedByAccount(@Param("ownedByAccount") boolean ownedByAccount);

    @RestResource(path = "findListByPersonalAccountId", rel = "findListByPersonalAccountId")
    List<AccountPromocode> findByPersonalAccountId(@Param("personalAccountId") String personalAccountId);
    Page<AccountPromocode> findByPersonalAccountId(@Param("personalAccountId") String personalAccountId, Pageable pageable);

    @RestResource(path = "findListByOwnerPersonalAccountId", rel = "findListByOwnerPersonalAccountId")
    List<AccountPromocode> findByOwnerPersonalAccountId(@Param("ownerPersonalAccountId") String ownerPersonalAccountId);
    Page<AccountPromocode> findByOwnerPersonalAccountId(@Param("ownerPersonalAccountId") String ownerPersonalAccountId, Pageable pageable);

    @RestResource(path = "findListByOwnerPersonalAccountIdAndPersonalAccountIdNot", rel = "findListByOwnerPersonalAccountIdAndPersonalAccountIdNot")
    List<AccountPromocode> findByOwnerPersonalAccountIdAndPersonalAccountIdNot(
            @Param("ownerPersonalAccountId") String ownerPersonalAccountId,
            @Param("personalAccountId") String personalAccountId
    );
    Page<AccountPromocode> findByOwnerPersonalAccountIdAndPersonalAccountIdNot(
            @Param("ownerPersonalAccountId") String ownerPersonalAccountId,
            @Param("personalAccountId") String personalAccountId,
            Pageable pageable
    );

    AccountPromocode findByPersonalAccountIdAndId(@Param("personalAccountId") String personalAccountId, @Param("id") String id);
}