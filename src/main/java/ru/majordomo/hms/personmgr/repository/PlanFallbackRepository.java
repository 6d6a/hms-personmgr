package ru.majordomo.hms.personmgr.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import ru.majordomo.hms.personmgr.model.plan.PlanFallback;

public interface PlanFallbackRepository extends MongoRepository<PlanFallback, String> {
    PlanFallback findOneByPlanId(String planId);
}
