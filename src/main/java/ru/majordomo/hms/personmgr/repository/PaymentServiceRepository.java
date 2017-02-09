package ru.majordomo.hms.personmgr.repository;


import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.querydsl.QueryDslPredicateExecutor;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.stream.Stream;

import ru.majordomo.hms.personmgr.common.ServicePaymentType;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.model.service.PaymentService;

public interface PaymentServiceRepository extends MongoRepository<PaymentService, String>,
        QueryDslPredicateExecutor<PaymentService> {
    @Cacheable("paymentServices")
    PaymentService findOne(String id);
    List<PaymentService> findAll();
    @Cacheable("paymentServicesActive")
    List<PaymentService> findByActive(@Param("active") boolean active);
    List<PaymentService> findByPaymentType(@Param("paymentType") ServicePaymentType paymentType);
    PaymentService findByName(@Param("name") String name);
    Stream<PaymentService> findByOldIdRegex(@Param("oldId") String oldId);
    @Cacheable("paymentServicesOldId")
    PaymentService findByOldId(@Param("oldId") String oldId);

    @Override
    @CachePut({"paymentServices", "paymentServicesOldId", "paymentServicesActive"})
    <S extends PaymentService> List<S> save(Iterable<S> entites);

    @Override
    @CachePut({"paymentServices", "paymentServicesOldId", "paymentServicesActive"})
    <S extends PaymentService> S save(S entity);

    @Override
    @CacheEvict({"paymentServices", "paymentServicesOldId", "paymentServicesActive"})
    void delete(Iterable<? extends PaymentService> entities);

    @Override
    @CacheEvict({"paymentServices", "paymentServicesOldId", "paymentServicesActive"})
    void delete(PaymentService entity);

    @Override
    @CacheEvict({"paymentServices", "paymentServicesOldId", "paymentServicesActive"})
    void delete(String s);

    @Override
    @CacheEvict(value = {"paymentServices", "paymentServicesOldId", "paymentServicesActive"}, allEntries = true)
    void deleteAll();
}