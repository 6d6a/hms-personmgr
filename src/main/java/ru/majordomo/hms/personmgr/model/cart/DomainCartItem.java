package ru.majordomo.hms.personmgr.model.cart;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.hibernate.validator.constraints.NotBlank;
import org.springframework.data.annotation.Transient;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessAction;
import ru.majordomo.hms.personmgr.model.promotion.AccountPromotion;
import ru.majordomo.hms.personmgr.strategy.CartItemStrategy;
import ru.majordomo.hms.personmgr.strategy.DomainCartItemStrategy;

public class DomainCartItem implements CartItem {
    @Transient
    @JsonIgnore
    private final String TYPE_TRANSLATED = "Домен";

    @NotBlank
    private String name;

    @NotBlank
    private String personId;

    private Boolean autoRenew = false;

    private Boolean processing = false;

    @Transient
    @JsonIgnore
    private DomainCartItemStrategy strategy;

    @Transient
    private BigDecimal price;

    @Transient
    @JsonIgnore
    private AccountPromotion accountPromotion;

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPersonId() {
        return personId;
    }

    public void setPersonId(String personId) {
        this.personId = personId;
    }

    public Boolean getAutoRenew() {
        return autoRenew;
    }

    public void setAutoRenew(Boolean autoRenew) {
        this.autoRenew = autoRenew;
    }

    @Override
    public Boolean getProcessing() {
        return processing;
    }

    @Override
    public void setProcessing(Boolean processing) {
        this.processing = processing;
    }

    public CartItemStrategy getStrategy() {
        return strategy;
    }

    public void setStrategy(DomainCartItemStrategy strategy) {
        this.strategy = strategy;
    }

    @Override
    public List<ProcessingBusinessAction> buy() {
        if (strategy != null) {
            return Collections.singletonList(strategy.buy(this));
        } else {
            throw new ParameterValidationException("There is no CartItemStrategy for this item");
        }
    }

    @Override
    public BigDecimal getPrice() {
        if (strategy != null) {
            if (price == null) {
                price = strategy.getPrice(this);
            }
        }

        return price;
    }

    @Override
    public String getTypeTranslated() {
        return TYPE_TRANSLATED;
    }

    @Override
    public AccountPromotion getAccountPromotion() {
        return accountPromotion;
    }

    @Override
    public void setAccountPromotion(AccountPromotion accountPromotion) {
        this.accountPromotion = accountPromotion;
    }

    @Override
    public String getAccountPromotionName() {
        return accountPromotion != null && accountPromotion.getPromotion() != null ? accountPromotion.getPromotion().getName() : null;
    }

    @Override
    public String getAccountPromotionId() {
        return accountPromotion != null ? accountPromotion.getId() : null;
    }

    @Override
    public String toString() {
        return "DomainCartItem{" +
                "name='" + name + '\'' +
                ", personId='" + personId + '\'' +
                ", autoRenew=" + autoRenew +
                ", strategy=" + strategy +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DomainCartItem that = (DomainCartItem) o;

        if (!name.equals(that.name)) return false;
        if (!personId.equals(that.personId)) return false;
        return autoRenew != null ? autoRenew.equals(that.autoRenew) : that.autoRenew == null;
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + personId.hashCode();
        result = 31 * result + (autoRenew != null ? autoRenew.hashCode() : 0);
        return result;
    }
}
