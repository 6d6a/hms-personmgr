package ru.majordomo.hms.personmgr.repository;


import org.springframework.data.repository.CrudRepository;

import java.util.List;

import ru.majordomo.hms.personmgr.common.FlowType;
import ru.majordomo.hms.personmgr.model.BusinessFlow;

public interface BusinessFlowRedisRepository extends CrudRepository<BusinessFlow, String> {
    BusinessFlow findOne(String id);

    List<BusinessFlow> findAll();

    BusinessFlow findByName(String name);

    BusinessFlow findByFlowType(FlowType flowType);
}