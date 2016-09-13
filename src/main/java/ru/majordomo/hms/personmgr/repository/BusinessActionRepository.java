package ru.majordomo.hms.personmgr.repository;


import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

import ru.majordomo.hms.personmgr.common.ActionType;
import ru.majordomo.hms.personmgr.model.BusinessAction;

public interface BusinessActionRepository extends MongoRepository<BusinessAction, String> {
    BusinessAction findOne(String id);

    List<BusinessAction> findAll();

    BusinessAction findByName(String name);

    BusinessAction findByActionType(@Param("actionType") ActionType actionType);
}