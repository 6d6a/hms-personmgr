package ru.majordomo.hms.personmgr.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import ru.majordomo.hms.personmgr.model.promotion.Promotion;

import java.util.List;

public interface PromotionRepository extends MongoRepository<Promotion, String> {
    Promotion findByName(String name);
    Promotion findByActionIdsIn(List<String> actionIds);
}
