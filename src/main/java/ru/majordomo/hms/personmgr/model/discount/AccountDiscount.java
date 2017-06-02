package ru.majordomo.hms.personmgr.model.discount;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

import org.springframework.data.annotation.Transient;

import java.time.LocalDateTime;

import javax.validation.constraints.NotNull;

import ru.majordomo.hms.personmgr.validation.ObjectId;


/**
 * AccountDiscount
 */
public class AccountDiscount {
    @NotNull
    @ObjectId(Discount.class)
    private String discountId;

    @Transient
    private Discount discount;

    @NotNull
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    private LocalDateTime created;

    public String getDiscountId() {
        return discountId;
    }

    public void setDiscountId(String discountId) {
        this.discountId = discountId;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public void setCreated(LocalDateTime created) {
        this.created = created;
    }

    public Discount getDiscount() {
        return discount;
    }

    public void setDiscount(Discount discount) {
        this.discount = discount;
    }

    @Override
    public String toString() {
        return "AccountDiscount{" +
                "discountId='" + discountId + '\'' +
                ", created=" + created +
                '}';
    }
}
