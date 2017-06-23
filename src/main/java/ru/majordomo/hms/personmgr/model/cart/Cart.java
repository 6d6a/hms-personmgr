package ru.majordomo.hms.personmgr.model.cart;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import javax.validation.constraints.NotNull;

import ru.majordomo.hms.personmgr.common.Views;
import ru.majordomo.hms.personmgr.model.ModelBelongsToPersonalAccount;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.strategy.DomainCartItemStrategy;
import ru.majordomo.hms.personmgr.validation.ObjectId;

@Document
//@TypeAlias("Cart")
public class Cart extends ModelBelongsToPersonalAccount implements CartItem {
    private Set<CartItem> items = new HashSet<>();

    @Transient
    @JsonIgnore
    private DomainCartItemStrategy domainCartItemStrategy;

    public Cart() {
        recalculate();
    }

    public Set<CartItem> getItems() {
        return items;
    }

    public void setItems(Set<CartItem> items) {
        this.items = items;
        recalculate();
    }

    public void addItem(CartItem item) {
        if (!items.contains(item)) {
            items.add(item);
            recalculate();
        }
    }

    public void removeItem(CartItem item) {
        items.remove(item);
        recalculate();
    }

    public void removeItems() {
        items.clear();
        recalculate();
    }

    @Override
    public String getName() {
        return "Корзина";
    }

    @Override
    public void buy() {
        items.forEach(CartItem::buy);
    }

    @Override
    public BigDecimal getPrice() {
        return items.stream().filter(item -> item.getPrice() != null).map(CartItem::getPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void recalculate() {
//        DomainCartItemStrategy domainCartItemStrategy = new DomainCartItemStrategy();
//        domainCartItemStrategy.setAccountId(getPersonalAccountId());

        if (domainCartItemStrategy != null) {
            items
                    .stream()
                    .filter(item -> item instanceof DomainCartItem)
                    .forEach(item -> ((DomainCartItem) item).setStrategy(domainCartItemStrategy))
            ;
        }
    }

    public DomainCartItemStrategy getDomainCartItemStrategy() {
        return domainCartItemStrategy;
    }

    public void setDomainCartItemStrategy(DomainCartItemStrategy domainCartItemStrategy) {
        this.domainCartItemStrategy = domainCartItemStrategy;
        recalculate();
    }

    @Override
    public String toString() {
        return "Cart{" +
                "items=" + items +
                ", domainCartItemStrategy=" + domainCartItemStrategy +
                "} " + super.toString();
    }
}
