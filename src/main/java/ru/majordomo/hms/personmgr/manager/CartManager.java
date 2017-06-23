package ru.majordomo.hms.personmgr.manager;

import java.util.List;

import ru.majordomo.hms.personmgr.model.cart.Cart;

public interface CartManager {
    boolean exists(String id);

    long count();

    void delete(String id);

    void delete(Cart cart);

    void delete(Iterable<Cart> carts);

    void deleteAll();

    Cart save(Cart cart);

    List<Cart> save(Iterable<Cart> carts);

    Cart insert(Cart cart);

    List<Cart> insert(Iterable<Cart> carts);

    Cart findOne(String id);

    List<Cart> findAll();

    Cart findByPersonalAccountId(String personalAccountId);
}
