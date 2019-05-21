package ru.majordomo.hms.personmgr.service;

import org.springframework.stereotype.Component;
import ru.majordomo.hms.personmgr.common.Utils;
import ru.majordomo.hms.personmgr.model.discount.*;
import ru.majordomo.hms.personmgr.model.promocode.PromocodeAction;

import java.math.BigDecimal;

@Component
public class DiscountFactory {
    public Discount getDiscount(PromocodeAction action) {
        Discount discount = new DiscountExactCost();

        switch (action.getActionType()) {
            case SERVICE_DISCOUNT:
                switch ((String) action.getProperties().get("type")) {
                    case "percent":
                        discount = new DiscountPercent();
                        break;
                    case "absolute":
                        discount = new DiscountAbsolute();
                        break;
                    case "fix":
                        discount = new DiscountFixCost();
                        break;
                }
                discount.setAmount(
                        Utils.getBigDecimalFromUnexpectedInput(action.getProperties().get("amount"))
                );

                break;
            case SERVICE_FREE_DOMAIN:
                discount = new DiscountFixCost();
                discount.setAmount(BigDecimal.ZERO);
                break;

            case SERVICE_DOMAIN_DISCOUNT_RU_RF:
                discount = new DiscountFixCost();
                discount.setAmount(
                        Utils.getBigDecimalFromUnexpectedInput(action.getProperties().get("cost"))
                );

        }
        return discount;
    }
}
