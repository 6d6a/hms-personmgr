package ru.majordomo.hms.personmgr.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import ru.majordomo.hms.personmgr.manager.AccountHistoryManager;
import ru.majordomo.hms.personmgr.manager.AccountPromotionManager;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.promocode.PromocodeAction;
import ru.majordomo.hms.personmgr.model.promotion.AccountPromotion;
import ru.majordomo.hms.personmgr.model.promotion.Promotion;
import ru.majordomo.hms.personmgr.service.promotion.AccountPromotionFactory;

@Service
@AllArgsConstructor
public class GiftHelper {
    private final AccountPromotionManager accountPromotionManager;
    private final AccountPromotionFactory accountPromotionFactory;
    private final AccountHistoryManager history;

    public void giveGift(PersonalAccount account, Promotion promotion) {
        for (PromocodeAction action : promotion.getActions()) {

            Long currentCount = accountPromotionManager.countByPersonalAccountIdAndPromotionIdAndActionId(
                    account.getId(), promotion.getId(), action.getId()
            );

            String description = action.getDescription() != null ? action.getDescription() : promotion.getName();

            if (currentCount < promotion.getLimitPerAccount() || promotion.getLimitPerAccount() == -1) {

                AccountPromotion accountPromotion = accountPromotionFactory.build(account, promotion, action);

                accountPromotionManager.insert(accountPromotion);

                history.save(account, "Добавлен бонус " + description);
            } else {
                history.save(account, "Бонус не добавлен. Превышен лимит '" + promotion.getLimitPerAccount() + "' на " + description);
            }
        }
    }
}
