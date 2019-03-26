package ru.majordomo.hms.personmgr.model.discount;

import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Document(collection = "discount")
public class DiscountPercent extends Discount {
    @Override
    public BigDecimal getCost(BigDecimal cost) {
        BigDecimal percentAmount = this.getAmount();

        BigDecimal subtract = cost.subtract(
                cost.multiply(
                        percentAmount.multiply(new BigDecimal("0.01"))
                )
        ).setScale(2, RoundingMode.FLOOR);

        return subtract.compareTo(cost) > 0 ? cost : subtract;
    }
}
