package ru.majordomo.hms.personmgr.repository;

import org.springframework.data.mongodb.repository.MongoRepository;


import java.util.List;

import ru.majordomo.hms.personmgr.common.PromocodeActionType;
import ru.majordomo.hms.personmgr.model.promocode.PromocodeAction;

public interface PromocodeActionRepository extends MongoRepository<PromocodeAction, String> {
    List<PromocodeAction> findByActionType(PromocodeActionType actionType);
}