package ru.majordomo.hms.personmgr.repository;


import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.stream.Stream;

import ru.majordomo.hms.personmgr.common.ServicePaymentType;
import ru.majordomo.hms.personmgr.model.service.PaymentService;

public interface PaymentServiceRepository extends MongoRepository<PaymentService, String> {
    @Cacheable("paymentServices")
    PaymentService findOne(String id);
    List<PaymentService> findAll();
    List<PaymentService> findByPaymentType(@Param("paymentType") ServicePaymentType paymentType);
    PaymentService findByName(@Param("name") String name);
    Stream<PaymentService> findByOldIdRegex(@Param("oldId") String oldId);
    @Cacheable("paymentServices")
    PaymentService findByOldId(@Param("oldId") String oldId);

    @Override
    @CachePut("paymentServices")
    <S extends PaymentService> List<S> save(Iterable<S> entites);

    @Override
    @CachePut("paymentServices")
    <S extends PaymentService> S save(S entity);

    @Override
    @CacheEvict("paymentServices")
    void delete(Iterable<? extends PaymentService> entities);

    @Override
    @CacheEvict("paymentServices")
    void delete(PaymentService entity);

    @Override
    @CacheEvict("paymentServices")
    void delete(String s);

    @Override
    @CacheEvict(value = "paymentServices", allEntries = true)
    void deleteAll();
}