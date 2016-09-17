package ru.majordomo.hms.personmgr.repository;


import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

import ru.majordomo.hms.personmgr.common.AccountType;
import ru.majordomo.hms.personmgr.model.plan.Plan;

public interface PlanRepository extends MongoRepository<Plan, String> {
    Plan findOne(String id);
    List<Plan> findAll();
    Plan findByName(String name);
    List<Plan> findByAccountType(AccountType accountType);
    Plan findByFinServiceId(String finServiceId);
}