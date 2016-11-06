package ru.majordomo.hms.personmgr.repository;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

import ru.majordomo.hms.personmgr.common.BusinessActionType;
import ru.majordomo.hms.personmgr.common.State;
import ru.majordomo.hms.personmgr.model.ProcessingBusinessAction;

public interface ProcessingBusinessActionRepository extends MongoRepository<ProcessingBusinessAction, String> {
    ProcessingBusinessAction findOne(String id);

    List<ProcessingBusinessAction> findAll();

    ProcessingBusinessAction findByName(String name);

    ProcessingBusinessAction findByBusinessActionType(BusinessActionType businessActionType);

    ProcessingBusinessAction findFirstByStateOrderByPriorityAscCreatedDateAsc(State state);

    Page<ProcessingBusinessAction> findByPersonalAccountId(@Param("accountId") String accountId, Pageable pageable);

    ProcessingBusinessAction findByIdAndPersonalAccountId(@Param("id") String id, @Param("accountId") String accountId);
}