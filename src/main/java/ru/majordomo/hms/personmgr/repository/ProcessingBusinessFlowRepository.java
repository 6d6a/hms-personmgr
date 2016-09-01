package ru.majordomo.hms.personmgr.repository;


import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

import ru.majordomo.hms.personmgr.common.FlowType;
import ru.majordomo.hms.personmgr.common.State;
import ru.majordomo.hms.personmgr.model.ProcessingBusinessFlow;

public interface ProcessingBusinessFlowRepository extends MongoRepository<ProcessingBusinessFlow, String> {
    ProcessingBusinessFlow findOne(String id);

    List<ProcessingBusinessFlow> findAll();

    ProcessingBusinessFlow findByName(String name);

    ProcessingBusinessFlow findByFlowType(FlowType flowType);

    ProcessingBusinessFlow findFirstByStateOrderByPriorityAscCreatedDateAsc(State state);
}