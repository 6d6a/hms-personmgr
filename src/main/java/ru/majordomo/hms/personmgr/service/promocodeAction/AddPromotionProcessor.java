package ru.majordomo.hms.personmgr.service.promocodeAction;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.majordomo.hms.personmgr.dto.Result;
import ru.majordomo.hms.personmgr.manager.AccountPromotionManager;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.promocode.PromocodeAction;
import ru.majordomo.hms.personmgr.model.promotion.AccountPromotion;
import ru.majordomo.hms.personmgr.model.promotion.Promotion;
import ru.majordomo.hms.personmgr.repository.PromotionRepository;
import ru.majordomo.hms.personmgr.service.AccountHelper;

import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class AddPromotionProcessor implements PromocodeActionProcessor {
    private final PromotionRepository promotionRepository;
    private final AccountPromotionManager accountPromotionManager;
    private final AccountHelper accountHelper;

    public AddPromotionProcessor(
            PromotionRepository promotionRepository,
            AccountPromotionManager accountPromotionManager,
            AccountHelper accountHelper
    ) {
        this.promotionRepository = promotionRepository;
        this.accountPromotionManager = accountPromotionManager;
        this.accountHelper = accountHelper;
    }

    @Override
    public Result process(PersonalAccount account, PromocodeAction action, String code) {
        log.debug("Processing promocode action: " + action.toString());

        Optional<Promotion> byId = getPromotion(action);

        if (!byId.isPresent()) {
            return Result.error("Не найден промоушен");
        }

        Promotion promotion = byId.get();

        List<AccountPromotion> accountPromotions = accountPromotionManager.findByPersonalAccountIdAndPromotionId(account.getId(), promotion.getId());

        String desc = promotion.getDescription() != null ? promotion.getDescription() : promotion.getName();

        if (accountPromotions == null || accountPromotions.isEmpty()) {

            accountHelper.giveGift(account, promotion);

            return Result.success();
        } else {
            return Result.error("Бонус " + desc + " уже добавлен на аккаунт");
        }
    }

    @Override
    public boolean isAllowed(PersonalAccount account, PromocodeAction action) {
        Optional<Promotion> byId = getPromotion(action);

        if (!byId.isPresent()) {
            return false;
        }

        Promotion promotion = byId.get();

        List<AccountPromotion> accountPromotions = accountPromotionManager
                .findByPersonalAccountIdAndPromotionId(account.getId(), promotion.getId());

        return accountPromotions == null || accountPromotions.isEmpty();
    }

    private Optional<Promotion> getPromotion(PromocodeAction action) {
        String promotionId = (String) action.getProperties().get("promotionId");

        return promotionRepository.findById(promotionId);
    }
}
