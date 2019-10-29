package ru.majordomo.hms.personmgr.repository;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;


import java.util.List;
import java.util.Optional;

import ru.majordomo.hms.personmgr.common.AccountType;
import ru.majordomo.hms.personmgr.model.plan.Plan;

public interface PlanRepository extends MongoRepository<Plan, String>,
        QuerydslPredicateExecutor<Plan> {
    @Cacheable("plansById")
    Optional<Plan> findById(String id);
    @Cacheable("plans")
    List<Plan> findAll();
    @Cacheable("plansByActive")
    List<Plan> findByActive(boolean active);
    @Cacheable("plansByName")
    Plan findByName(String name);
    @Cacheable("plansByAccountType")
    List<Plan> findByAccountType(AccountType accountType);
    @Cacheable("plansByServiceId")
    Plan findByServiceId(String serviceId);
    @Cacheable("plansByOldId")
    Plan findByOldId(String oldId);
    @Cacheable("plansByAbonementIds")
    Plan findByAbonementIds(String abonementIds);

    @Override
    @CachePut("plans")
    <S extends Plan> List<S> saveAll(Iterable<S> entites);

    @Override
    @CachePut("plans")
    <S extends Plan> S save(S entity);

    @Override
    @CacheEvict("plans")
    void deleteAll(Iterable<? extends Plan> entities);

    @Override
    @CacheEvict("plans")
    void delete(Plan entity);

    @Override
    @CacheEvict("plans")
    void deleteById(String s);

    @Override
    @CacheEvict(value = "plans", allEntries = true)
    void deleteAll();


}