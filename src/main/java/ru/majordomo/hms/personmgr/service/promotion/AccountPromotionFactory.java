package ru.majordomo.hms.personmgr.service.promotion;

import org.springframework.stereotype.Service;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.promocode.PromocodeAction;
import ru.majordomo.hms.personmgr.model.promotion.AccountPromotion;
import ru.majordomo.hms.personmgr.model.promotion.Promotion;

import java.time.LocalDateTime;
import java.time.Period;
import java.util.Optional;

@Service
public class AccountPromotionFactory {

    public AccountPromotion build(PersonalAccount account, Promotion promotion, PromocodeAction action) {
        Optional<Period> validPeriod = (action.getProperties().get("validPeriod") instanceof String)
                ? Optional.of(Period.parse((String) action.getProperties().get("validPeriod")))
                : Optional.empty();

        AccountPromotion accountPromotion = new AccountPromotion();
        accountPromotion.setPersonalAccountId(account.getId());
        accountPromotion.setPromotionId(promotion.getId());
        accountPromotion.setPromotion(promotion);
        accountPromotion.setCreated(LocalDateTime.now());
        accountPromotion.setActionId(action.getId());
        accountPromotion.setAction(action);
        accountPromotion.setActive(true);

        validPeriod.ifPresent(period -> accountPromotion.setValidUntil(LocalDateTime.now().plus(period)));

        return accountPromotion;
    }
}
