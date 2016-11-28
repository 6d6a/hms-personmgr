package ru.majordomo.hms.personmgr.repository;


import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

import ru.majordomo.hms.personmgr.common.AccountType;
import ru.majordomo.hms.personmgr.model.plan.Plan;

public interface PlanRepository extends MongoRepository<Plan, String> {
    @Cacheable("plans")
    Plan findOne(String id);
    List<Plan> findAll();
    @Cacheable("plans")
    List<Plan> findByActive(@Param("active") boolean active);
    @Cacheable("plans")
    Plan findByName(@Param("name") String name);
    @Cacheable("plans")
    List<Plan> findByAccountType(@Param("accountType") AccountType accountType);
    @Cacheable("plans")
    Plan findByServiceId(@Param("serviceId") String serviceId);
    @Cacheable("plans")
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
    @CacheEvict("plans")
    void deleteAll();
}