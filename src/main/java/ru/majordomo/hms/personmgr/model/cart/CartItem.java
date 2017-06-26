package ru.majordomo.hms.personmgr.model.cart;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.math.BigDecimal;

import ru.majordomo.hms.personmgr.model.promocode.PromocodeAction;

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

    default PromocodeAction getPromocodeAction() {
        return null;
    }

    default void setPromocodeAction(PromocodeAction promocodeAction) {}
}
