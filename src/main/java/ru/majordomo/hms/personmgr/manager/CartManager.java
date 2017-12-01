package ru.majordomo.hms.personmgr.manager;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessAction;
import ru.majordomo.hms.personmgr.model.cart.Cart;
import ru.majordomo.hms.personmgr.model.cart.CartItem;

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

    Cart addCartItem(String accountId, CartItem cartItem);

    Cart deleteCartItemByName(String accountId, String cartItemName);

    Cart setCartItems(String accountId, Set<CartItem> cartItems);

    void setProcessing(String accountId, boolean status);

    void setProcessingByName(String accountId, String name, boolean status);

    List<ProcessingBusinessAction> buy(String accountId, BigDecimal cartPrice);

    List<Cart> findNotEmptyCartsAtLastMonth();
}
