package ru.majordomo.hms.personmgr.repository;

import feign.Param;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

import ru.majordomo.hms.personmgr.model.promotion.Promotion;

public interface PromotionRepository extends MongoRepository<Promotion, String> {
    Promotion findOne(String id);
    List<Promotion> findAll();
    Promotion findByName(@Param("name") String name);
}
