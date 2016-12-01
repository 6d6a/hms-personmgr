package ru.majordomo.hms.personmgr.model.discount;

import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;

/**
 * DiscountAbsolute
 */
@Document(collection = "discount")
public class DiscountAbsolute extends Discount {
    @Override
    public BigDecimal getCost(BigDecimal cost) {
        return cost.subtract(this.getAmount());
    }
}
