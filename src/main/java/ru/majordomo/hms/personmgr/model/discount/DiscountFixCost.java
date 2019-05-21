package ru.majordomo.hms.personmgr.model.discount;

import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;

/**
 * DiscountExactCost
 */
@Document(collection = "discount")
public class DiscountFixCost extends Discount {
    @Override
    public BigDecimal getCost(BigDecimal cost) {
        return getAmount();
    }
}
