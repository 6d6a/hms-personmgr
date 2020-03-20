package ru.majordomo.hms.personmgr.manager;

import java.util.List;
import java.util.Optional;

import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.promotion.AccountPromotion;
import ru.majordomo.hms.personmgr.model.service.PaymentService;

public interface AccountPromotionManager {
    boolean exists(String id);

    long count();

    AccountPromotion save(AccountPromotion accountPromotion);

    AccountPromotion insert(AccountPromotion accountPromotion);

    AccountPromotion findOne(String id);

    List<AccountPromotion> findAll();

    List<AccountPromotion> findByPersonalAccountId(String personalAccountId);

    List<AccountPromotion> findByPersonalAccountIdAndActive(String personalAccountId, boolean active);

    List<AccountPromotion> findByPersonalAccountIdAndPromotionId(String personalAccountId, String promotionId);

    List<AccountPromotion> findByPersonalAccountIdAndActionIdInAndActive(String personalAccountId, List<String> actionIds, boolean active);

    Long countByPersonalAccountIdAndPromotionIdAndActionId(String personalAccountId, String promotionId, String actionId);

    void setAsActiveAccountPromotionById(String id);

    void setAsUsedAccountPromotionById(String id);

    void setAsUsedAccountPromotionById(String id, String comment);

    AccountPromotion getServiceDiscountPromotion(String accountId, PaymentService service);

    boolean existsByPersonalAccountIdAndPromotionId(String personalAccountId, String promotionId);

    Optional<AccountPromotion> findByIdAndPersonalAccountId(String id, String accountId);
}
