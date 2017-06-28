package ru.majordomo.hms.personmgr.model.cart;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import ru.majordomo.hms.personmgr.model.VersionedModelBelongsToPersonalAccount;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessAction;
import ru.majordomo.hms.personmgr.model.promotion.AccountPromotion;
import ru.majordomo.hms.personmgr.strategy.DomainCartItemStrategy;

@Document
public class Cart extends VersionedModelBelongsToPersonalAccount implements CartItem {
    @Transient
    @JsonIgnore
    private final String TYPE = "Корзина";

    private Set<CartItem> items = new HashSet<>();

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
        return items.size() != 0 && items.stream().allMatch(CartItem::getProcessing);
    }

    @Override
    public void setProcessing(Boolean processing) {}

    @Override
    @JsonIgnore
    public String getName() {
        return TYPE;
    }

    @Override
    @JsonIgnore
    public String getType() {
        return TYPE;
    }

    @Override
    public List<ProcessingBusinessAction> buy() {
        return items
                .stream()
                .map(CartItem::buy)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
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
    @JsonIgnore
    public AccountPromotion getAccountPromotion() {
        return null;
    }

    @Override
    @JsonIgnore
    public String getAccountPromotionName() {
        return null;
    }

    @Override
    @JsonIgnore
    public String getAccountPromotionId() {
        return null;
    }

    @Override
    public String toString() {
        return "Cart{" +
                "items=" + items +
                ", domainCartItemStrategy=" + domainCartItemStrategy +
                "} " + super.toString();
    }
}
