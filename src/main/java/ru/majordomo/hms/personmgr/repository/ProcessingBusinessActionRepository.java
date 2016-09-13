package ru.majordomo.hms.personmgr.repository;


import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

import ru.majordomo.hms.personmgr.common.ActionType;
import ru.majordomo.hms.personmgr.common.State;
import ru.majordomo.hms.personmgr.model.ProcessingBusinessAction;

public interface ProcessingBusinessActionRepository extends MongoRepository<ProcessingBusinessAction, String> {
    ProcessingBusinessAction findOne(String id);

    List<ProcessingBusinessAction> findAll();

    ProcessingBusinessAction findByName(String name);

    ProcessingBusinessAction findByActionType(ActionType actionType);

    ProcessingBusinessAction findFirstByStateOrderByPriorityAscCreatedDateAsc(State state);
}