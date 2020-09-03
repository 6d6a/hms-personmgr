package ru.majordomo.hms.personmgr.dto;

import lombok.Data;
import ru.majordomo.hms.personmgr.model.promotion.AccountPromotion;

import javax.annotation.Nullable;
import java.math.BigDecimal;

@Data
public class DomainPriceInfo {
    @Nullable
    private BigDecimal price;
    private BigDecimal priceWithoutDiscount;

    /** можно ли зарегистрировать домен, свободен ли домен */
    private boolean free;

    private boolean premium;
    @Nullable
    private AccountPromotion accountPromotion;
}
