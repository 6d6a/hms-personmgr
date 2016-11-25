package ru.majordomo.hms.personmgr.repository;


import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

import ru.majordomo.hms.personmgr.model.discount.Discount;

public interface DiscountRepository extends MongoRepository<Discount, String> {
    Discount findOne(String id);
    List<Discount> findAll();
    Discount findByName(@Param("name") String name);
}