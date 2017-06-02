package ru.majordomo.hms.personmgr.repository;

import feign.Param;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

import ru.majordomo.hms.personmgr.model.promotion.AccountPromotion;

public interface AccountPromotionRepository extends MongoRepository<AccountPromotion, String> {
    List<AccountPromotion> findByPersonalAccountId(@Param("personalAccountId") String personalAccountId);
    List<AccountPromotion> findByPersonalAccountIdAndPromotionId(@Param("personalAccountId") String personalAccountId, @Param("promotionId") String promotionId);
    Long countByPersonalAccountIdAndPromotionId(@Param("personalAccountId") String personalAccountId, @Param("promotionId") String promotionId);
}
