package ru.majordomo.hms.personmgr.manager.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.manager.AccountPromotionManager;
import ru.majordomo.hms.personmgr.manager.CartManager;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessAction;
import ru.majordomo.hms.personmgr.model.cart.Cart;
import ru.majordomo.hms.personmgr.model.cart.CartItem;
import ru.majordomo.hms.personmgr.model.cart.DomainCartItem;
import ru.majordomo.hms.personmgr.repository.CartRepository;
import ru.majordomo.hms.personmgr.service.DomainService;
import ru.majordomo.hms.personmgr.strategy.DomainCartItemStrategy;

import static ru.majordomo.hms.personmgr.common.Utils.formatBigDecimalWithCurrency;

@Component
public class CartManagerImpl implements CartManager {
    private final CartRepository repository;
    private final MongoOperations mongoOperations;
    private final DomainService domainService;
    private final AccountPromotionManager accountPromotionManager;

    @Autowired
    public CartManagerImpl(
            CartRepository repository,
            MongoOperations mongoOperations,
            DomainService domainService,
            AccountPromotionManager accountPromotionManager
    ) {
        this.repository = repository;
        this.mongoOperations = mongoOperations;
        this.domainService = domainService;
        this.accountPromotionManager = accountPromotionManager;
    }

    @Override
    public boolean exists(String id) {
        return repository.exists(id);
    }

    @Override
    public long count() {
        return repository.count();
    }

    @Override
    public void delete(String id) {
        repository.delete(id);
    }

    @Override
    public void delete(Cart cart) {
        repository.delete(cart);
    }

    @Override
    public void delete(Iterable<Cart> carts) {
        repository.delete(carts);
    }

    @Override
    public void deleteAll() {
        repository.deleteAll();
    }

    @Override
    public Cart save(Cart cart) {
        return repository.save(cart);
    }

    @Override
    public List<Cart> save(Iterable<Cart> carts) {
        return repository.save(carts);
    }

    @Override
    public Cart insert(Cart cart) {
        return repository.insert(cart);
    }

    @Override
    public List<Cart> insert(Iterable<Cart> carts) {
        return repository.insert(carts);
    }

    @Override
    public Cart findOne(String id) {
        checkById(id);
        Cart cart = repository.findOne(id);
        setDomainCartItemStrategy(cart);

        return cart;
    }

    @Override
    public List<Cart> findAll() {
        return repository
                .findAll()
                .stream()
                .map(cart -> {
                    setDomainCartItemStrategy(cart);
                    return cart;
                })
                .collect(Collectors.toList());
    }

    @Override
    public Cart findByPersonalAccountId(String personalAccountId) {
        Cart cart = repository.findByPersonalAccountId(personalAccountId);

        if (cart == null) {
            cart = new Cart();
            cart.setPersonalAccountId(personalAccountId);
            insert(cart);
        }

        setDomainCartItemStrategy(cart);

        return cart;
    }

    @Override
    @Retryable(include = {OptimisticLockingFailureException.class}, maxAttempts = 5)
    public Cart addCartItem(String accountId, CartItem cartItem) {
        Cart cart = findByPersonalAccountId(accountId);

        if (cart.getProcessing()) {
            throw new ParameterValidationException("Корзина находится в процессе покупки. Дождитесь окончания обработки.");
        }

        checkCartItem(cart, cartItem);

        if (cart.hasItem(cartItem)) {
            throw new ParameterValidationException(cartItem.getTypeTranslated() + " " + cartItem.getName() + " уже присутствует в корзине");
        }

        cart.addItem(cartItem);

        save(cart);

        return cart;
    }

    @Override
    @Retryable(include = {OptimisticLockingFailureException.class}, maxAttempts = 5)
    public Cart deleteCartItemByName(String accountId, String cartItemName) {
        Cart cart = findByPersonalAccountId(accountId);

        cart.getItems().stream().filter(item -> item.getName().equals(cartItemName)).findFirst().ifPresent(cart::removeItem);

        save(cart);

        return cart;
    }

    @Override
    @Retryable(include = {OptimisticLockingFailureException.class}, maxAttempts = 5)
    public Cart setCartItems(String accountId, Set<CartItem> cartItems) {
        Cart cart = findByPersonalAccountId(accountId);

        if (cart.getProcessing() && cartItems.size() != 0) {
            throw new ParameterValidationException("Корзина находится в процессе покупки. Дождитесь окончания обработки.");
        }

        cartItems.forEach(item -> checkCartItem(cart, item));

        cart.setItems(cartItems);

        save(cart);

        return cart;
    }

    @Override
    @Retryable(include = {OptimisticLockingFailureException.class}, maxAttempts = 5)
    public void setProcessing(String accountId, boolean status) {
        Cart cart = findByPersonalAccountId(accountId);

        cart.getItems().forEach(item -> item.setProcessing(status));

        save(cart);
    }

    @Override
    @Retryable(include = {OptimisticLockingFailureException.class}, maxAttempts = 5)
    public void setProcessingByName(String accountId, String name, boolean status) {
        Cart cart = findByPersonalAccountId(accountId);

        cart.getItems()
                .stream()
                .filter(item -> item.getName().equals(name))
                .findFirst()
                .ifPresent(item -> item.setProcessing(status))
        ;

        save(cart);
    }

    @Override
    public List<ProcessingBusinessAction> buy(String accountId, BigDecimal cartPrice) {
        Cart cart = findByPersonalAccountId(accountId);

        if (cart.getProcessing()) {
            throw new ParameterValidationException("Корзина находится в процессе покупки. Дождитесь окончания обработки.");
        }

        if (cart.getPrice().compareTo(cartPrice) != 0) {
            throw new ParameterValidationException("Переданная стоимость корзины не совпадает с её текущей стоимостью. " +
                    "Передано: " + formatBigDecimalWithCurrency(cartPrice) +
                    " Текущая: " + formatBigDecimalWithCurrency(cart.getPrice())
            );
        }

        setProcessing(accountId, true);

        try {
            return cart.buy();
        } catch (Exception e) {
            e.printStackTrace();
            setProcessing(accountId, false);
            throw e;
        }
    }

    private void checkCartItem(Cart cart, CartItem cartItem) {
        if (cartItem instanceof DomainCartItem) {
            cart.getDomainCartItemStrategy().check(cartItem);
        }
    }

    private void checkById(String id) {
        if (!exists(id)) {
            throw new ResourceNotFoundException("Cart с id: " + id + " не найден");
        }
    }

    private void setDomainCartItemStrategy(Cart cart) {
        if (cart == null) {
            return;
        }

        DomainCartItemStrategy domainCartItemStrategy = new DomainCartItemStrategy(
                cart.getPersonalAccountId(),
                domainService,
                accountPromotionManager
        );

        domainCartItemStrategy.reloadAccountPromotions();

        cart.setDomainCartItemStrategy(domainCartItemStrategy);
    }
}
