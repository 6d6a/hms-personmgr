package ru.majordomo.hms.personmgr.strategy;

import ru.majordomo.hms.personmgr.model.cart.CartItem;

public interface CartItemBuyStrategy {
    void buy(CartItem item);
}
