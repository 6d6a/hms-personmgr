package ru.majordomo.hms.personmgr.manager;

import java.util.List;

import ru.majordomo.hms.personmgr.model.promotion.AccountPromotion;

public interface AccountPromotionManager {
    boolean exists(String id);

    long count();

    void delete(String id);

    void delete(AccountPromotion accountPromotion);

    void delete(Iterable<AccountPromotion> accountPromotions);

    void deleteAll();

    AccountPromotion save(AccountPromotion accountPromotion);

    List<AccountPromotion> save(Iterable<AccountPromotion> accountPromotions);

    AccountPromotion insert(AccountPromotion accountPromotion);

    List<AccountPromotion> insert(Iterable<AccountPromotion> accountPromotions);

    AccountPromotion findOne(String id);

    List<AccountPromotion> findAll();

    List<AccountPromotion> findByPersonalAccountId(String personalAccountId);

    List<AccountPromotion> findByPersonalAccountIdAndPromotionId(String personalAccountId, String promotionId);

    Long countByPersonalAccountIdAndPromotionId(String personalAccountId, String promotionId);

    void activateAccountPromotionByIdAndActionId(String id, String actionId);

    void deactivateAccountPromotionByIdAndActionId(String id, String actionId);

    void switchAccountPromotionById(String id);
}
