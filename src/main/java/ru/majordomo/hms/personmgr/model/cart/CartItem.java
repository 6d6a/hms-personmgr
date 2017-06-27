package ru.majordomo.hms.personmgr.model.cart;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.math.BigDecimal;

import ru.majordomo.hms.personmgr.model.promotion.AccountPromotion;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
              include = JsonTypeInfo.As.PROPERTY,
              property = "type")
@JsonSubTypes({
                      @JsonSubTypes.Type(value = DomainCartItem.class,
                                         name = "domain"),
                      @JsonSubTypes.Type(value = Cart.class,
                                         name = "cart")
              })
public interface CartItem {
    String getName();

    Boolean getProcessing();

    void setProcessing(Boolean processing);

    void buy();

    BigDecimal getPrice();

    String getType();

    default void check() {}

    default AccountPromotion getAccountPromotion() {
        return null;
    }

    default void setAccountPromotion(AccountPromotion accountPromotion) {}

    default String getAccountPromotionName() {
        return null;
    }

    default String getAccountPromotionId() {
        return null;
    }
}
