package ru.majordomo.hms.personmgr.manager.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

import ru.majordomo.hms.personmgr.manager.AccountPromotionManager;
import ru.majordomo.hms.personmgr.manager.CartManager;
import ru.majordomo.hms.personmgr.model.cart.Cart;
import ru.majordomo.hms.personmgr.repository.CartRepository;
import ru.majordomo.hms.personmgr.service.DomainService;
import ru.majordomo.hms.personmgr.strategy.DomainCartItemStrategy;

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
        setDomainCartItemStrategy(cart);

        return cart;
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
