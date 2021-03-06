package ru.majordomo.hms.personmgr.repository;

import org.springframework.data.mongodb.repository.MongoRepository;


import ru.majordomo.hms.personmgr.common.BusinessActionType;
import ru.majordomo.hms.personmgr.model.business.BusinessAction;

public interface BusinessActionRepository extends MongoRepository<BusinessAction, String> {
    BusinessAction findByName(String name);

    BusinessAction findByBusinessActionType(BusinessActionType businessActionType);
}