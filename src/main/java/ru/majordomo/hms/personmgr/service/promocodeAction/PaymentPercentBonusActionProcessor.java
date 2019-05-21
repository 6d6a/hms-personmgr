package ru.majordomo.hms.personmgr.service.promocodeAction;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.majordomo.hms.personmgr.common.PromocodeActionType;
import ru.majordomo.hms.personmgr.dto.Result;
import ru.majordomo.hms.personmgr.dto.fin.PaymentRequest;
import ru.majordomo.hms.personmgr.manager.AccountHistoryManager;
import ru.majordomo.hms.personmgr.manager.AccountPromotionManager;
import ru.majordomo.hms.personmgr.model.BaseModel;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.promocode.PromocodeAction;
import ru.majordomo.hms.personmgr.model.promotion.AccountPromotion;
import ru.majordomo.hms.personmgr.model.promotion.Promotion;
import ru.majordomo.hms.personmgr.repository.PromocodeActionRepository;
import ru.majordomo.hms.personmgr.repository.PromotionRepository;
import ru.majordomo.hms.personmgr.service.AccountHelper;
import ru.majordomo.hms.personmgr.feign.FinFeignClient;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.*;
import java.util.stream.Collectors;

import static ru.majordomo.hms.personmgr.common.Utils.getBigDecimalFromUnexpectedInput;

@Slf4j
@Service
public class PaymentPercentBonusActionProcessor implements PromocodeActionProcessor {
    private static final String PROMOTION_NAME = "payment_percent_bonus";

    private final PromotionRepository promotionRepository;
    private final AccountPromotionManager accountPromotionManager;
    private final AccountHelper accountHelper;
    private final AccountHistoryManager history;
    private final PromocodeActionRepository promocodeActionRepository;
    private final FinFeignClient finFeignClient;

    @Autowired
    public PaymentPercentBonusActionProcessor(
            PromotionRepository promotionRepository,
            AccountPromotionManager accountPromotionManager,
            AccountHelper accountHelper,
            AccountHistoryManager history,
            PromocodeActionRepository promocodeActionRepository,
            FinFeignClient finFeignClient
    ) {
        this.promotionRepository = promotionRepository;
        this.accountPromotionManager = accountPromotionManager;
        this.accountHelper = accountHelper;
        this.history = history;
        this.promocodeActionRepository = promocodeActionRepository;
        this.finFeignClient = finFeignClient;
    }

    @Override
    public Result process(PersonalAccount account, PromocodeAction action, String code) {
        Promotion promotion = getPromotion();

        accountHelper.giveGift(account, promotion);

        history.save(
                account,
                "При использовании промокода '" + code + "' добавлен бонус на пополнение баланса"
        );
        return Result.success();
    }

    @Override
    public boolean isAllowed(PersonalAccount account, PromocodeAction action) {
        Promotion promotion = getPromotion();

        List<AccountPromotion> accountPromotions = accountPromotionManager
                .findByPersonalAccountIdAndPromotionId(account.getId(), promotion.getId());

        return accountPromotions == null || accountPromotions.isEmpty();
    }

    public void processPayment(PersonalAccount account, BigDecimal amount) {
        BigDecimal bonusPaymentAmount = calculateBonusPayment(account, amount);

        if (bonusPaymentAmount.compareTo(BigDecimal.ZERO) <= 0) { return; }

        try {
            finFeignClient.addPayment(
                    new PaymentRequest(account.getName())
                            .withAmount(bonusPaymentAmount)
                            .withBonusType()
                            .withMessage("Бонус за пополнение счета")
                            .withDisableAsync(true)
            );
            history.save(
                    account,
                    "Начислено " + bonusPaymentAmount + " бонусов после пополнения баланса при обработке бонуса");
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getClass().getName() + ": " + e.getMessage());
        }
    }

    private Promotion getPromotion() {
        return promotionRepository.findByName(PROMOTION_NAME);
    }

    private List<String> getActionIds() {
        List<PromocodeAction> actions = promocodeActionRepository.findByActionType(PromocodeActionType.PAYMENT_PERCENT_BONUS);
        if (actions == null) {
            return Collections.emptyList();
        } else {
            return actions.stream().map(BaseModel::getId).collect(Collectors.toList());
        }
    }

    private BigDecimal calculateBonusPayment(PersonalAccount account, BigDecimal amount) {
        try {
            List<AccountPromotion> accountPromotions = accountPromotionManager
                    .findByPersonalAccountIdAndActionIdInAndActive(account.getId(), getActionIds(), true);

            if (accountPromotions == null || accountPromotions.isEmpty()) {
                return BigDecimal.ZERO;
            }

            AccountPromotion accountPromotion = accountPromotions
                    .stream()
                    .filter(ap -> {
                        Period period = Period.parse(ap.getAction().getProperties().get("period").toString());
                        if (!ap.getCreated().plus(period).isBefore(LocalDateTime.now())) {
                            return true;
                        } else {
                            accountPromotionManager.setAsUsedAccountPromotionById(ap.getId());
                            return false;
                        }
                    })
                    .filter(ap -> {
                        if (ap.getAction().getProperties().get("minPaymentAmount") != null) {
                            return getBigDecimalFromUnexpectedInput(ap.getAction().getProperties().get("minPaymentAmount"))
                                    .compareTo(amount) <= 0;
                        } else {
                            return true;
                        }
                    }).max(
                            Comparator.comparing(
                                    ap -> getBigDecimalFromUnexpectedInput(
                                            ap.getAction().getProperties().get("percent")
                                    )
                            )
                    ).orElse(null);

            if (accountPromotion == null) {
                return BigDecimal.ZERO;
            }

            BigDecimal percent = getBigDecimalFromUnexpectedInput(accountPromotion.getAction().getProperties().get("percent"));

            return amount.multiply(percent);
        } catch (Exception e) {
            log.error(e.getClass().getName() + " e.message: " + e.getMessage());
            return BigDecimal.ZERO;
        }
    }
}
