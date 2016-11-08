package ru.majordomo.hms.personmgr.repository;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

import ru.majordomo.hms.personmgr.common.State;
import ru.majordomo.hms.personmgr.model.ProcessingBusinessOperation;

public interface ProcessingBusinessOperationRepository extends MongoRepository<ProcessingBusinessOperation, String> {
    ProcessingBusinessOperation findOne(String id);

    List<ProcessingBusinessOperation> findAll();

    ProcessingBusinessOperation findByState(@Param("state") State state);

    ProcessingBusinessOperation findFirstByStateOrderByPriorityAscCreatedDateAsc(@Param("state") State state);

    Page<ProcessingBusinessOperation> findByPersonalAccountId(@Param("accountId") String accountId, Pageable pageable);

    ProcessingBusinessOperation findByIdAndPersonalAccountIdAndStateIn(@Param("id") String id, @Param("accountId") String accountId, @Param("states") List<State> states);
}