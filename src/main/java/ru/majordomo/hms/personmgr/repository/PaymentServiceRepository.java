package ru.majordomo.hms.personmgr.repository;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import ru.majordomo.hms.personmgr.model.service.PaymentService;

public interface PaymentServiceRepository extends MongoRepository<PaymentService, String>,
        QuerydslPredicateExecutor<PaymentService> {
    @Cacheable("paymentServices")
    @Override
    Optional<PaymentService> findById(String id);

    List<PaymentService> findAll();

    @Query("{}")
    List<PaymentService> findAllPaymentServices();

    @Cacheable("paymentServicesActive")
    List<PaymentService> findByActive(boolean active);

    PaymentService findByName(String name);

    Stream<PaymentService> findByOldIdRegex(String oldId);

    @Cacheable("paymentServicesOldId")
    PaymentService findByOldId(String oldId);

    @Override
    @CachePut({"paymentServices", "paymentServicesOldId", "paymentServicesActive"})
    <S extends PaymentService> List<S> saveAll(Iterable<S> entites);

    @Override
    @CachePut({"paymentServices", "paymentServicesOldId", "paymentServicesActive"})
    <S extends PaymentService> S save(S entity);

    @Override
    @CacheEvict({"paymentServices", "paymentServicesOldId", "paymentServicesActive"})
    void delete(PaymentService entity);

    @Override
    @CacheEvict(value = {"paymentServices", "paymentServicesOldId", "paymentServicesActive"},
                allEntries = true)
    void deleteAll();
}