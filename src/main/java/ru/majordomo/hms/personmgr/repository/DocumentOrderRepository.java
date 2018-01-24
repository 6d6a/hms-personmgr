package ru.majordomo.hms.personmgr.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.repository.query.Param;
import ru.majordomo.hms.personmgr.model.account.DocumentOrder;


public interface DocumentOrderRepository extends MongoRepository<DocumentOrder, String> {
    Page<DocumentOrder> findByPersonalAccountId(@Param("accountId") String accountId, Pageable pageable);
    DocumentOrder findOneByIdAndPersonalAccountId(@Param("id") String id, @Param("personalAccountId") String personalAccountId);
}
