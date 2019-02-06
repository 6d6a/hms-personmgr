package ru.majordomo.hms.personmgr.repository;

import org.springframework.data.mongodb.repository.MongoRepository;


import ru.majordomo.hms.personmgr.model.discount.Discount;

public interface DiscountRepository extends MongoRepository<Discount, String> {
    Discount findByName(String name);
}