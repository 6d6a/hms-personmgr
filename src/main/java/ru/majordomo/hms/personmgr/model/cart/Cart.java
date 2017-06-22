package ru.majordomo.hms.personmgr.model.cart;

import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import ru.majordomo.hms.personmgr.model.ModelBelongsToPersonalAccount;

@Document
public class Cart extends ModelBelongsToPersonalAccount implements CartItem {
    private Set<CartItem> items = new HashSet<>();

    public Set<CartItem> getItems() {
        return items;
    }

    public void setItems(Set<CartItem> items) {
        this.items = items;
    }

    public void addItem(CartItem item) {
        items.add(item);
    }

    public void removeItem(CartItem item) {
        items.remove(item);
    }

    public void removeItems() {
        items.clear();
    }

    @Override
    public void buy() {
        items.forEach(CartItem::buy);
    }

    @Override
    public BigDecimal getPrice() {
        return items.stream().map(CartItem::getPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
