package ru.majordomo.hms.personmgr.model.cart;

import java.math.BigDecimal;

public interface CartItem {
    void buy();
    BigDecimal getPrice();
}
