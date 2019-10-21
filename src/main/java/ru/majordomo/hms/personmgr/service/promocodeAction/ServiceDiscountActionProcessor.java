package ru.majordomo.hms.personmgr.service.promocodeAction;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.majordomo.hms.personmgr.dto.Result;
import ru.majordomo.hms.personmgr.manager.AccountHistoryManager;
import ru.majordomo.hms.personmgr.manager.AccountPromotionManager;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.promocode.PromocodeAction;
import ru.majordomo.hms.personmgr.model.promotion.Promotion;
import ru.majordomo.hms.personmgr.repository.PromotionRepository;
import ru.majordomo.hms.personmgr.service.GiftHelper;

import java.util.Collections;

@Service
@Slf4j
public class ServiceDiscountActionProcessor implements PromocodeActionProcessor {

    private final PromotionRepository promotionRepository;
    private final AccountPromotionManager accountPromotionManager;
    private final AccountHistoryManager history;
    private final GiftHelper giftHelper;

    @Autowired
    public ServiceDiscountActionProcessor(
            PromotionRepository promotionRepository,
            AccountPromotionManager accountPromotionManager,
            AccountHistoryManager history,
            GiftHelper giftHelper
    ) {
        this.promotionRepository = promotionRepository;
        this.accountPromotionManager = accountPromotionManager;
        this.history = history;
        this.giftHelper = giftHelper;
    }

    @Override
    public Result process(PersonalAccount account, PromocodeAction action, String code) {
        log.debug("Processing promocode SERVICE_DISCOUNT codeAction: " + action.toString());

        Promotion promotion = getPromotion(action);

        boolean exists = accountPromotionManager.existsByPersonalAccountIdAndPromotionId(account.getId(), promotion.getId());

        if (!exists) {

            giftHelper.giveGift(account, promotion);

            history.save(
                    account,
                    "Добавлена скидка на сервис при использовании промокода " + code
            );
            return Result.success();
        } else {
            return Result.error("Скидка уже добавлена на аккаунт");
        }
    }

    @Override
    public boolean isAllowed(PersonalAccount account, PromocodeAction action) {
        Promotion promotion = getPromotion(action);
        
        return !accountPromotionManager.existsByPersonalAccountIdAndPromotionId(account.getId(), promotion.getId());
    }

    private Promotion getPromotion(PromocodeAction action) {
        return promotionRepository.findByActionIdsIn(Collections.singletonList(action.getId()));
    }
}
