package ru.majordomo.hms.personmgr.repository;

import feign.Param;
import org.springframework.data.mongodb.repository.MongoRepository;

import ru.majordomo.hms.personmgr.model.promotion.Promotion;

public interface PromotionRepository extends MongoRepository<Promotion, String> {
    Promotion findByName(@Param("name") String name);
}
