package ru.majordomo.hms.personmgr.model.cart;

import org.hibernate.validator.constraints.NotBlank;
import org.springframework.data.annotation.Transient;

import java.math.BigDecimal;

public class DomainCartItem implements CartItem {
    @NotBlank
    private String name;

    @NotBlank
    private String personId;

    private Boolean autoRenew = false;

//    @Transient
//    private
    @Override
    public void buy() {

    }

    @Override
    public BigDecimal getPrice() {
        return null;
    }
}
