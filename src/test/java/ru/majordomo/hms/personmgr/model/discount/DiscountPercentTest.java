package ru.majordomo.hms.personmgr.model.discount;

import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.*;

public class DiscountPercentTest {

    @Test
    public void getCost() {
        Discount discount;

        discount = new DiscountPercent();
        discount.setAmount(new BigDecimal("10"));
        assertEquals(new BigDecimal("90.00"), discount.getCost(new BigDecimal("100")));

        discount = new DiscountPercent();
        discount.setAmount(new BigDecimal("1"));
        assertEquals(new BigDecimal("4.95"), discount.getCost(new BigDecimal("5")));

        discount = new DiscountPercent();
        discount.setAmount(new BigDecimal("0.00001"));
        assertEquals(new BigDecimal("4.98"), discount.getCost(new BigDecimal("4.99")));
    }
}