package ru.majordomo.hms.personmgr.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import ru.majordomo.hms.personmgr.model.account.DocumentOrder;


public interface DocumentOrderRepository extends MongoRepository<DocumentOrder, String> { }
