package ru.majordomo.hms.personmgr.repository;


import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

import ru.majordomo.hms.personmgr.common.FlowType;
import ru.majordomo.hms.personmgr.model.BusinessFlow;

public interface BusinessFlowRepository extends MongoRepository<BusinessFlow, String> {
    BusinessFlow findOne(String id);

    List<BusinessFlow> findAll();

    BusinessFlow findByName(String name);

    BusinessFlow findByFlowType(@Param("flowType") FlowType flowType);
}