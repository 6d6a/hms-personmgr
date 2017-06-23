package ru.majordomo.hms.personmgr.strategy;

import java.math.BigDecimal;

import ru.majordomo.hms.personmgr.model.cart.CartItem;

public interface CartItemStrategy {
    void buy(CartItem item);
    BigDecimal getPrice(CartItem item);
}
