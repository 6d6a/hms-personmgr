package ru.majordomo.hms.personmgr.service.promocodeAction;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.majordomo.hms.personmgr.dto.Result;
import ru.majordomo.hms.personmgr.manager.AccountHistoryManager;
import ru.majordomo.hms.personmgr.manager.AccountPromotionManager;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.promocode.PromocodeAction;
import ru.majordomo.hms.personmgr.model.promotion.AccountPromotion;
import ru.majordomo.hms.personmgr.model.promotion.Promotion;
import ru.majordomo.hms.personmgr.repository.PromotionRepository;
import ru.majordomo.hms.personmgr.service.AccountHelper;

import java.util.List;

import static ru.majordomo.hms.personmgr.common.Constants.FREE_DOMAIN_PROMOTION;

@Service
@Slf4j
public class FreeDomainActionProcessor implements PromocodeActionProcessor {

    private final PromotionRepository promotionRepository;
    private final AccountPromotionManager accountPromotionManager;
    private final AccountHelper accountHelper;
    private final AccountHistoryManager history;

    @Autowired
    public FreeDomainActionProcessor(
            PromotionRepository promotionRepository,
            AccountPromotionManager accountPromotionManager,
            AccountHelper accountHelper,
            AccountHistoryManager history
    ) {
        this.promotionRepository = promotionRepository;
        this.accountPromotionManager = accountPromotionManager;
        this.accountHelper = accountHelper;
        this.history = history;
    }

    @Override
    public Result process(PersonalAccount account, PromocodeAction action, String code) {
        log.debug("Processing promocode SERVICE_FREE_DOMAIN codeAction: " + action.toString());

        Promotion promotion = getPromotion();

        List<AccountPromotion> accountPromotions = accountPromotionManager.findByPersonalAccountIdAndPromotionId(account.getId(), promotion.getId());

        if (accountPromotions == null || accountPromotions.isEmpty()) {

            accountHelper.giveGift(account, promotion);

            history.save(
                    account,
                    "Добавлен бонус на регистрацию бесплатного домена RU, РФ при использовании промокода " + code
            );
            return Result.success();
        } else {
            return Result.error("Бонус на регистрацию бесплатного домена RU, РФ уже добавлен на аккаунт");
        }
    }

    @Override
    public boolean isAllowed(PersonalAccount account, PromocodeAction action) {
        Promotion promotion = getPromotion();
        
        List<AccountPromotion> accountPromotions = accountPromotionManager
                .findByPersonalAccountIdAndPromotionId(account.getId(), promotion.getId());

        return accountPromotions == null || accountPromotions.isEmpty();
    }

    private Promotion getPromotion() {
        return promotionRepository.findByName(FREE_DOMAIN_PROMOTION);
    }
}
