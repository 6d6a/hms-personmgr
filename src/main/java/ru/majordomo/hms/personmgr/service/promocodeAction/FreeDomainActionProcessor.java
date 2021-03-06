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

import static ru.majordomo.hms.personmgr.common.Constants.FREE_DOMAIN_PROMOTION;

@Service
@Slf4j
public class FreeDomainActionProcessor implements PromocodeActionProcessor {

    private final PromotionRepository promotionRepository;
    private final AccountPromotionManager accountPromotionManager;
    private final AccountHistoryManager history;
    private final GiftHelper giftHelper;

    @Autowired
    public FreeDomainActionProcessor(
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
        log.debug("Processing promocode SERVICE_FREE_DOMAIN codeAction: " + action.toString());

        Promotion promotion = getPromotion();

        boolean exists = accountPromotionManager.existsByPersonalAccountIdAndPromotionId(account.getId(), promotion.getId());

        if (!exists) {

            giftHelper.giveGift(account, promotion);

            history.save(
                    account,
                    "???????????????? ?????????? ???? ?????????????????????? ?????????????????????? ???????????? RU, ???? ?????? ?????????????????????????? ?????????????????? " + code
            );
            return Result.success();
        } else {
            return Result.error("?????????? ???? ?????????????????????? ?????????????????????? ???????????? RU, ???? ?????? ???????????????? ???? ??????????????");
        }
    }

    @Override
    public boolean isAllowed(PersonalAccount account, PromocodeAction action) {
        Promotion promotion = getPromotion();
        
        return !accountPromotionManager.existsByPersonalAccountIdAndPromotionId(account.getId(), promotion.getId());
    }

    private Promotion getPromotion() {
        return promotionRepository.findByName(FREE_DOMAIN_PROMOTION);
    }
}
