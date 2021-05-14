package ru.majordomo.hms.personmgr.repository;

import org.springframework.data.mongodb.repository.*;
import ru.majordomo.hms.personmgr.model.account.*;

import javax.annotation.Nullable;

public interface DocumentOrderCountRepository extends MongoRepository<DocumentOrderCount, String> {
    @Nullable
    DocumentOrderCount findByPersonalAccountId(String accountId);
}
