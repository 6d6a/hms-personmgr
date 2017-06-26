package ru.majordomo.hms.personmgr.model.cart;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import ru.majordomo.hms.personmgr.model.ModelBelongsToPersonalAccount;
import ru.majordomo.hms.personmgr.strategy.DomainCartItemStrategy;

@Document
public class Cart extends ModelBelongsToPersonalAccount implements CartItem {
    @Transient
    @JsonIgnore
    private final String TYPE = "Корзина";

    private Set<CartItem> items = new HashSet<>();

    private Boolean processing = false;

    @Transient
    @JsonIgnore
    private DomainCartItemStrategy domainCartItemStrategy;

    @Transient
    private BigDecimal price;

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

    public boolean hasItem(CartItem item) {
        return items.contains(item);
    }

    @Override
    public Boolean getProcessing() {
        return processing;
    }

    @Override
    public void setProcessing(Boolean processing) {
        this.processing = processing;
    }

    @Override
    public String getName() {
        return TYPE;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public void buy() {
        items.forEach(CartItem::buy);
    }

    @Override
    public BigDecimal getPrice() {
        if (price == null) {
            price = items.stream().filter(item -> item.getPrice() != null).map(CartItem::getPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        return price;
    }

    private void recalculate() {
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
