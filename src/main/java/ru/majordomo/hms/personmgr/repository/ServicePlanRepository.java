package ru.majordomo.hms.personmgr.repository;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.querydsl.QueryDslPredicateExecutor;
import org.springframework.data.repository.query.Param;
import ru.majordomo.hms.personmgr.model.plan.Feature;
import ru.majordomo.hms.personmgr.model.plan.ServicePlan;

import java.util.List;

public interface ServicePlanRepository extends MongoRepository<ServicePlan, String>, QueryDslPredicateExecutor<ServicePlan> {
    @Cacheable("servicePlans")
    ServicePlan findOne(String id);
    List<ServicePlan> findAll();

    @Cacheable("servicePlans")
    ServicePlan findByServiceId(@Param("serviceId") String serviceId, @Param("active") boolean active);

    @Override
    @CachePut("servicePlans")
    <S extends ServicePlan> List<S> save(Iterable<S> entites);

    @Override
    @CachePut("servicePlans")
    <S extends ServicePlan> S save(S entity);

    @Override
    @CacheEvict("servicePlans")
    void delete(Iterable<? extends ServicePlan> entities);

    @Override
    @CacheEvict("servicePlans")
    void delete(ServicePlan entity);

    @Override
    @CacheEvict("servicePlans")
    void delete(String s);

    @Override
    @CacheEvict(value = "servicePlans", allEntries = true)
    void deleteAll();

    @CacheEvict("servicePlans")
    List<ServicePlan> findAllByFeature(@Param("feature") Feature feature);

    @CacheEvict("servicePlans")
    ServicePlan findOneByFeatureAndActive(@Param("feature") Feature feature, @Param("active") boolean active);
}