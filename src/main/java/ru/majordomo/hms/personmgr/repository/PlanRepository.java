package ru.majordomo.hms.personmgr.repository;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.querydsl.QueryDslPredicateExecutor;
import org.springframework.data.repository.query.Param;

import java.util.List;

import ru.majordomo.hms.personmgr.common.AccountType;
import ru.majordomo.hms.personmgr.model.plan.Plan;

public interface PlanRepository extends MongoRepository<Plan, String>,
        QueryDslPredicateExecutor<Plan> {
    @Cacheable("plansById")
    Plan findOne(String id);
    @Cacheable("plans")
    List<Plan> findAll();
    @Cacheable("plansByActive")
    List<Plan> findByActive(@Param("active") boolean active);
    @Cacheable("plansByName")
    Plan findByName(@Param("name") String name);
    @Cacheable("plansByAccountType")
    List<Plan> findByAccountType(@Param("accountType") AccountType accountType);
    @Cacheable("plansByServiceId")
    Plan findByServiceId(@Param("serviceId") String serviceId);
    @Cacheable("plansByOldId")
    Plan findByOldId(@Param("oldId") String oldId);

    @Override
    @CachePut("plans")
    <S extends Plan> List<S> save(Iterable<S> entites);

    @Override
    @CachePut("plans")
    <S extends Plan> S save(S entity);

    @Override
    @CacheEvict("plans")
    void delete(Iterable<? extends Plan> entities);

    @Override
    @CacheEvict("plans")
    void delete(Plan entity);

    @Override
    @CacheEvict("plans")
    void delete(String s);

    @Override
    @CacheEvict(value = "plans", allEntries = true)
    void deleteAll();
}