package ru.majordomo.hms.personmgr.repository;


import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.stream.Stream;

import ru.majordomo.hms.personmgr.common.ServicePaymentType;
import ru.majordomo.hms.personmgr.model.service.PaymentService;

public interface PaymentServiceRepository extends MongoRepository<PaymentService, String> {
    PaymentService findOne(String id);
    List<PaymentService> findAll();
    List<PaymentService> findByPaymentType(@Param("paymentType") ServicePaymentType paymentType);
    PaymentService findByName(@Param("name") String name);
    Stream<PaymentService> findByOldIdRegex(@Param("oldId") String oldId);
    PaymentService findByOldId(@Param("oldId") String oldId);
}