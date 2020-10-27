package ru.majordomo.hms.personmgr.manager.impl;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.exception.ResourceNotFoundException;
import ru.majordomo.hms.personmgr.feign.RcUserFeignClient;
import ru.majordomo.hms.personmgr.manager.AccountPromotionManager;
import ru.majordomo.hms.personmgr.manager.CartManager;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessAction;
import ru.majordomo.hms.personmgr.model.cart.Cart;
import ru.majordomo.hms.personmgr.model.cart.CartItem;
import ru.majordomo.hms.personmgr.model.cart.DomainCartItem;
import ru.majordomo.hms.personmgr.repository.CartRepository;
import ru.majordomo.hms.personmgr.service.DomainService;
import ru.majordomo.hms.personmgr.strategy.DomainCartItemStrategy;
import ru.majordomo.hms.rc.user.resources.Person;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.match;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation;
import static ru.majordomo.hms.personmgr.common.Utils.formatBigDecimalWithCurrency;

@Component
@RequiredArgsConstructor
public class CartManagerImpl implements CartManager {
    private final CartRepository repository;
    private final MongoOperations mongoOperations;
    private final DomainService domainService;
    private final AccountPromotionManager accountPromotionManager;
    private final RcUserFeignClient rcUserFeignClient;

    @Override
    public boolean exists(String id) {
        return repository.existsById(id);
    }

    @Override
    public long count() {
        return repository.count();
    }

    @Override
    public void delete(String id) {
        repository.deleteById(id);
    }

    @Override
    public void delete(Cart cart) {
        repository.delete(cart);
    }

    @Override
    public void delete(Iterable<Cart> carts) {
        repository.deleteAll(carts);
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
        return repository.saveAll(carts);
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
        Cart cart = repository.findById(id).orElse(null);
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
    public List<ProcessingBusinessAction> buy(String accountId, BigDecimal cartPrice) throws ParameterValidationException {
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

        Set<String> personIds = rcUserFeignClient.getPersons(accountId).stream().map(Person::getId)
                .filter(StringUtils::isNotEmpty).collect(Collectors.toSet());

        if (cart.getItems().stream().filter(cartItem -> cartItem instanceof DomainCartItem)
                .map(cartItem -> (DomainCartItem) cartItem)
                .anyMatch(domainCartItem -> !personIds.contains(domainCartItem.getPersonId()))) {
            throw new ParameterValidationException("Некорректная персона");
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

    @Override
    public List<Cart> findNotEmptyCartsAtLastMonth(){
        return mongoOperations.aggregate(/*Date.from(startDateTime.toInstant(ZoneOffset.ofHours(3)))*/
                newAggregation(
                        match(new Criteria().andOperator(
                                Criteria.where("items.0").exists(true),
                                Criteria.where("updateDateTime").gt(Date.from(LocalDateTime.now().minusMonths(1).toInstant(ZoneOffset.ofHours(3)))))
                )), "cart", Cart.class)
                .getMappedResults();
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

    @Override
    @Retryable(include = {OptimisticLockingFailureException.class}, maxAttempts = 5)
    public Cart setCartPersonId(String accountId, String personId) {
        Cart cart = findByPersonalAccountId(accountId);
        for (CartItem cartItem : cart.getItems()) {
            if (cartItem instanceof DomainCartItem) {
                ((DomainCartItem) cartItem).setPersonId(personId);
            }
        }
        save(cart);
        return cart;
    }
}
