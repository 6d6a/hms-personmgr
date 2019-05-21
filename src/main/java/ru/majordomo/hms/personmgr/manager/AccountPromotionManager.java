package ru.majordomo.hms.personmgr.manager;

import java.util.List;

import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.promotion.AccountPromotion;
import ru.majordomo.hms.personmgr.model.service.PaymentService;

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

    List<AccountPromotion> findByPersonalAccountIdAndActive(String personalAccountId, boolean active);

    List<AccountPromotion> findByPersonalAccountIdAndPromotionId(String personalAccountId, String promotionId);

    List<AccountPromotion> findByPersonalAccountIdAndActionIdInAndActive(String personalAccountId, List<String> actionIds, boolean active);

    Long countByPersonalAccountIdAndPromotionIdAndActionId(String personalAccountId, String promotionId, String actionId);

    void setAsActiveAccountPromotionById(String id);

    void setAsUsedAccountPromotionById(String id);

    AccountPromotion getServiceDiscountPromotion(PersonalAccount account, PaymentService service);

    boolean existsByPersonalAccountIdAndPromotionId(String personalAccountId, String promotionId);
}
