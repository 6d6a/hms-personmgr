package ru.majordomo.hms.personmgr.controller.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestWrapper;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Set;

import javax.validation.Valid;

import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.manager.CartManager;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.cart.Cart;
import ru.majordomo.hms.personmgr.model.cart.CartItem;
import ru.majordomo.hms.personmgr.validation.ObjectId;

import static ru.majordomo.hms.personmgr.common.Utils.formatBigDecimalWithCurrency;

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
        Cart cart = manager.addCartItem(accountId, cartItem);

        return new ResponseEntity<>(cart.getItems(), HttpStatus.OK);
    }

    @RequestMapping(value = "/items", method = RequestMethod.PATCH)
    public ResponseEntity<Set<CartItem>> setCartItems(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @Valid @RequestBody Set<CartItem> cartItems,
            SecurityContextHolderAwareRequestWrapper request
    ) {
        Cart cart = manager.setCartItems(accountId, cartItems);

        return new ResponseEntity<>(cart.getItems(), HttpStatus.OK);
    }

    @RequestMapping(value = "/buy", method = RequestMethod.POST)
    public ResponseEntity<Void> buy(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @RequestParam(value = "price") BigDecimal cartPrice
    ) {
        PersonalAccount account = accountManager.findOne(accountId);

        if (!account.isActive()) {
            throw new ParameterValidationException("Аккаунт неактивен. Покупки услуг невозможны.");
        }

        Cart cart = manager.findByPersonalAccountId(accountId);

        if (!cart.getPrice().equals(cartPrice)) {
            throw new ParameterValidationException("Переданная стоимость корзины не совпадает с её текущей стоимостью. " +
                    "Передано: " + formatBigDecimalWithCurrency(cartPrice) +
                    " Текущая: " + formatBigDecimalWithCurrency(cart.getPrice())
            );
        }

        cart.buy();

        manager.setProcessing(accountId, true);

        return new ResponseEntity<>(HttpStatus.OK);
    }
}
