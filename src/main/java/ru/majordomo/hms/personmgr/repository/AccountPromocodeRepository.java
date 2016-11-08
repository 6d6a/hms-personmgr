package ru.majordomo.hms.personmgr.repository;


import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

import ru.majordomo.hms.personmgr.model.promocode.AccountPromocode;

public interface AccountPromocodeRepository extends MongoRepository<AccountPromocode, String> {
    AccountPromocode findOne(String id);
    List<AccountPromocode> findAll();
    List<AccountPromocode> findByPromocodeId(@Param("promocodeId") String promocodeId);
    AccountPromocode findOneByPromocodeId(@Param("promocodeId") String promocodeId);
    AccountPromocode findByPromocodeIdAndOwnedByAccount(@Param("promocodeId") String promocodeId, @Param("ownedByAccount") boolean ownedByAccount);
    List<AccountPromocode> findByOwnedByAccount(@Param("ownedByAccount") boolean ownedByAccount);
    List<AccountPromocode> findByPersonalAccountId(@Param("personalAccountId") String personalAccountId);
}