package ru.majordomo.hms.personmgr.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.repository.query.Param;

import ru.majordomo.hms.personmgr.model.cart.Cart;

public interface CartRepository extends MongoRepository<Cart, String> {
    Cart findByPersonalAccountId(@Param("personalAccountId") String personalAccountId);
}
