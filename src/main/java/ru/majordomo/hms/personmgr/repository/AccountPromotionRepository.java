package ru.majordomo.hms.personmgr.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

import ru.majordomo.hms.personmgr.model.promotion.AccountPromotion;

public interface AccountPromotionRepository extends MongoRepository<AccountPromotion, String> {
    List<AccountPromotion> findByPersonalAccountId(String personalAccountId);
    List<AccountPromotion> findByPersonalAccountIdAndPromotionId(String personalAccountId, String promotionId);
    boolean existsByPersonalAccountIdAndPromotionId(String personalAccountId, String promotionId);
    Long countByPersonalAccountIdAndPromotionIdAndActionId(String personalAccountId, String promotionId, String actionId);

    List<AccountPromotion> findByPersonalAccountIdAndActionIdInAndActive(String personalAccountId, List<String> actionIds, boolean active);

    List<AccountPromotion> findByPersonalAccountIdAndActive(String personalAccountId, boolean active);

    Optional<AccountPromotion> findByIdAndPersonalAccountId(String id, String accountId);
}
