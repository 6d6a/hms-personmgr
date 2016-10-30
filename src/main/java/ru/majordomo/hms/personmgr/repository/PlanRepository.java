package ru.majordomo.hms.personmgr.repository;


import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

import ru.majordomo.hms.personmgr.common.AccountType;
import ru.majordomo.hms.personmgr.model.plan.Plan;

public interface PlanRepository extends MongoRepository<Plan, String> {
    Plan findOne(String id);
    List<Plan> findAll();
    List<Plan> findByActive(@Param("active") boolean active);
    Plan findByName(@Param("name") String name);
    List<Plan> findByAccountType(@Param("accountType") AccountType accountType);
    Plan findByFinServiceId(@Param("finServiceId") String finServiceId);
}