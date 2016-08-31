package ru.majordomo.hms.personmgr.repository;


import org.springframework.data.repository.CrudRepository;

import java.util.List;

import ru.majordomo.hms.personmgr.common.FlowType;
import ru.majordomo.hms.personmgr.model.BusinessFlow;
import ru.majordomo.hms.personmgr.model.ProcessingBusinessFlow;

public interface ProcessingBusinessFlowRepository extends CrudRepository<ProcessingBusinessFlow, String> {
    ProcessingBusinessFlow findOne(String id);

    List<ProcessingBusinessFlow> findAll();

    ProcessingBusinessFlow findByName(String name);

    ProcessingBusinessFlow findByFlowType(FlowType flowType);
}