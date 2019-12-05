package ru.majordomo.hms.personmgr.repository;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

import ru.majordomo.hms.personmgr.model.plan.Feature;
import ru.majordomo.hms.personmgr.model.plan.ServicePlan;

import java.util.List;
import java.util.Optional;

public interface ServicePlanRepository extends MongoRepository<ServicePlan, String>, QuerydslPredicateExecutor<ServicePlan> {
    @Cacheable("servicePlansById")
    Optional<ServicePlan> findById(String id);

    @Cacheable("servicePlans")
    List<ServicePlan> findAll();

    @Override
    @CachePut("servicePlans")
    <S extends ServicePlan> S save(S entity);

    @Override
    @CacheEvict("servicePlans")
    void deleteAll(Iterable<? extends ServicePlan> entities);

    @Override
    @CacheEvict("servicePlans")
    void delete(ServicePlan entity);

    @Override
    @CacheEvict("servicePlans")
    void deleteById(String s);

    @Override
    @CacheEvict(value = "servicePlans", allEntries = true)
    void deleteAll();

    @Cacheable("oneServicePlanByFeatureAndActive")
    ServicePlan findOneByFeatureAndActive(Feature feature, boolean active);

    @Cacheable("oneServicePlanByFeatureAndServiceId")
    ServicePlan findOneByFeatureAndServiceId(Feature feature, String serviceId);

    @Cacheable("oneServicePlanByFeature")
    ServicePlan findOneByFeature(Feature feature);

    @Cacheable("servicePlansByPaymentServiceId")
    List<ServicePlan> findByServiceId(String ServiceId);
}