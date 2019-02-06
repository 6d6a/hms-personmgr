package ru.majordomo.hms.personmgr.repository;

import com.querydsl.core.types.Predicate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

import java.util.List;

import ru.majordomo.hms.personmgr.model.promocode.AccountPromocode;

public interface AccountPromocodeRepository extends MongoRepository<AccountPromocode, String>,
        QuerydslPredicateExecutor<AccountPromocode> {
    Page<AccountPromocode> findAll(Predicate predicate, Pageable pageable);
    AccountPromocode findOneByPromocodeId(String promocodeId);

    List<AccountPromocode> findByPersonalAccountId(String personalAccountId);
    Page<AccountPromocode> findByPersonalAccountId(String personalAccountId, Pageable pageable);

    AccountPromocode findByPersonalAccountIdAndId(String personalAccountId, String id);

    void deleteByOwnerPersonalAccountId(String ownerPersonalAccountId);

    boolean existsByPromocodeId(String promocodeId);
}