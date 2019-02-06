package ru.majordomo.hms.personmgr.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import ru.majordomo.hms.personmgr.model.account.DocumentOrder;


public interface DocumentOrderRepository extends MongoRepository<DocumentOrder, String> {
    Page<DocumentOrder> findByPersonalAccountId(String accountId, Pageable pageable);
    DocumentOrder findOneByIdAndPersonalAccountId(String id, String personalAccountId);
}
