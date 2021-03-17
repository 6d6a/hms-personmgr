package ru.majordomo.hms.personmgr.repository;

import org.springframework.data.mongodb.repository.*;
import ru.majordomo.hms.personmgr.model.account.*;


public interface DocumentOrderCountRepository extends MongoRepository<DocumentOrderCount, String> {
    DocumentOrderCount findByPersonalAccountId(String accountId);
}
