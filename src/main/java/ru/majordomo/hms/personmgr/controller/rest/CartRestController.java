package ru.majordomo.hms.personmgr.controller.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestWrapper;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

import javax.validation.Valid;

import ru.majordomo.hms.personmgr.manager.CartManager;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.cart.Cart;
import ru.majordomo.hms.personmgr.model.cart.CartItem;
import ru.majordomo.hms.personmgr.validation.ObjectId;

@RestController
@RequestMapping("/{accountId}/cart")
public class CartRestController extends CommonRestController {
    private final CartManager manager;

    @Autowired
    public CartRestController(CartManager manager) {
        this.manager = manager;
    }

    @RequestMapping(value = "", method = RequestMethod.GET)
    public ResponseEntity<Cart> get(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId
    ) {
        Cart cart = manager.findByPersonalAccountId(accountId);

        return new ResponseEntity<>(cart, HttpStatus.OK);
    }

    @RequestMapping(value = "/items", method = RequestMethod.POST)
    public ResponseEntity<Set<CartItem>> addCartItem(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @Valid @RequestBody CartItem cartItem,
            SecurityContextHolderAwareRequestWrapper request
    ) {
        Cart cart = manager.findByPersonalAccountId(accountId);

        if (cart == null) {
            cart = new Cart();
            cart.setPersonalAccountId(accountId);
            manager.save(cart);
        }

        cart.addItem(cartItem);

        manager.save(cart);

        return new ResponseEntity<>(cart.getItems(), HttpStatus.OK);
    }
}
