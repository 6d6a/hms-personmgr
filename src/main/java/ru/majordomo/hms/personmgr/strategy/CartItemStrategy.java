package ru.majordomo.hms.personmgr.strategy;

import java.math.BigDecimal;

import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessAction;
import ru.majordomo.hms.personmgr.model.cart.CartItem;
import ru.majordomo.hms.personmgr.model.promotion.AccountPromotion;

public interface CartItemStrategy {
    ProcessingBusinessAction buy(CartItem item);

    BigDecimal getPrice(CartItem item);

    default void check(CartItem domain) {
    }

    AccountPromotion usePromotion(CartItem domain);
}
