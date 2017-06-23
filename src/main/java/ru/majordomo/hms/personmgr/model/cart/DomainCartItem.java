package ru.majordomo.hms.personmgr.model.cart;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.hibernate.validator.constraints.NotBlank;
import org.springframework.data.annotation.Transient;
import org.springframework.data.annotation.TypeAlias;

import java.math.BigDecimal;

import ru.majordomo.hms.personmgr.exception.DomainNotAvailableException;
import ru.majordomo.hms.personmgr.strategy.CartItemStrategy;
import ru.majordomo.hms.personmgr.strategy.DomainCartItemStrategy;

//@TypeAlias("DomainCartItem")
public class DomainCartItem implements CartItem {
    @NotBlank
    private String name;

    @NotBlank
    private String personId;

    private Boolean autoRenew = false;

    @Transient
    @JsonIgnore
    private DomainCartItemStrategy strategy;

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

    public CartItemStrategy getStrategy() {
        return strategy;
    }

    public void setStrategy(DomainCartItemStrategy strategy) {
        this.strategy = strategy;
    }

    @Override
    public void buy() {
        if (strategy != null) {
            strategy.buy(this);
        }
    }

    @Override
    public BigDecimal getPrice() {
        if (strategy != null) {
            return strategy.getPrice(this);
        } else {
            return null;
//            throw new DomainNotAvailableException("No strategy specified for domainCartItem");
        }
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
