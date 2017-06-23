package ru.majordomo.hms.personmgr.model.cart;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.math.BigDecimal;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
                      @JsonSubTypes.Type(value = DomainCartItem.class, name = "domain"),
                      @JsonSubTypes.Type(value = Cart.class, name = "cart")
              })
public interface CartItem {
    String getName();
    void buy();
    BigDecimal getPrice();
}
