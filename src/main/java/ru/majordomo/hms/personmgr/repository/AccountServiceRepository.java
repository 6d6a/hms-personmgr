package ru.majordomo.hms.personmgr.repository;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

import ru.majordomo.hms.personmgr.model.service.AccountService;

public interface AccountServiceRepository extends MongoRepository<AccountService,String> {
    AccountService findOne(String id);
    List<AccountService> findAll();
    AccountService findByPaymentAccountIdAndId(@Param("paymentAccountId") String paymentAccountId, @Param("id") String id);
    Page<AccountService> findByPaymentAccountId(@Param("paymentAccountId") String paymentAccountId, Pageable pageable);
}