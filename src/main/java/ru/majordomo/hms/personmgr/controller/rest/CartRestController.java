package ru.majordomo.hms.personmgr.controller.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestWrapper;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;

import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.manager.CartManager;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessAction;
import ru.majordomo.hms.personmgr.model.cart.Cart;
import ru.majordomo.hms.personmgr.model.cart.CartItem;
import ru.majordomo.hms.personmgr.validation.ObjectId;

@RestController
@RequestMapping("/{accountId}/cart")
@Validated
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
    public ResponseEntity<Cart> addCartItem(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @Valid @RequestBody CartItem cartItem,
            SecurityContextHolderAwareRequestWrapper request
    ) {
        Cart cart = manager.addCartItem(accountId, cartItem);

        return new ResponseEntity<>(cart, HttpStatus.OK);
    }

    @DeleteMapping("/items")
    public ResponseEntity<Cart> deleteCartItem(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @RequestParam @NotBlank String name,
            SecurityContextHolderAwareRequestWrapper request
    ) {
        Cart cart = manager.deleteCartItemByName(accountId, name);

        return new ResponseEntity<>(cart, HttpStatus.OK);
    }

    @RequestMapping(value = "/items", method = RequestMethod.PATCH)
    public ResponseEntity<Cart> setCartItems(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @Valid @RequestBody Set<CartItem> cartItems,
            SecurityContextHolderAwareRequestWrapper request
    ) {
        Cart cart = manager.setCartItems(accountId, cartItems);

        return new ResponseEntity<>(cart, HttpStatus.OK);
    }

    @RequestMapping(value = "/buy", method = RequestMethod.POST)
    public ResponseEntity<List<SimpleServiceMessage>> buy(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @RequestBody Map<String, BigDecimal> requestBody
    ) {
        PersonalAccount account = accountManager.findOne(accountId);

        if (!account.isActive()) {
            throw new ParameterValidationException("?????????????? ??????????????????. ?????????????? ?????????? ????????????????????.");
        }

        if (account.isFreeze()) {
            throw new ParameterValidationException("?????????????? ??????????????????. ?????????????? ?????????? ????????????????????.");
        }

        BigDecimal cartPrice = requestBody.getOrDefault("price", BigDecimal.ZERO);

        List<ProcessingBusinessAction> processingBusinessActions = manager.buy(accountId, cartPrice);

        List<SimpleServiceMessage> simpleServiceMessages = processingBusinessActions
                .stream()
                .map(this::createSuccessResponse)
                .collect(Collectors.toList());

        return new ResponseEntity<>(simpleServiceMessages, HttpStatus.OK);
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    @RequestMapping(value = "/processing", method = RequestMethod.PATCH)
    public ResponseEntity<Cart> unlockCart(@ObjectId(PersonalAccount.class) @PathVariable String accountId) {
        manager.setProcessing(accountId, false);
        return ResponseEntity.ok(manager.findByPersonalAccountId(accountId));
    }

    @PatchMapping(value = "/items/fields")
    public ResponseEntity<Cart> setCartItemsPersonId(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @RequestParam String personId,
            SecurityContextHolderAwareRequestWrapper request
    ) {
        Cart cart = manager.setCartPersonId(accountId, personId);

        return new ResponseEntity<>(cart, HttpStatus.OK);
    }
}
