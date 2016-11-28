package ru.majordomo.hms.personmgr.model.discount;

import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * DiscountPercent
 */
@Document(collection = "discount")
public class DiscountPercent extends Discount {
    @Override
    public BigDecimal getCost(BigDecimal cost) {
        return cost.subtract(cost.multiply(this.getAmount().divide(BigDecimal.valueOf(100), RoundingMode.FLOOR)));
    }
}
