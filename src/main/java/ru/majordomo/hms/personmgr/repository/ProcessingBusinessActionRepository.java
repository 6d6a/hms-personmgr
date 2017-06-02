package ru.majordomo.hms.personmgr.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.stream.Stream;

import ru.majordomo.hms.personmgr.common.BusinessActionType;
import ru.majordomo.hms.personmgr.common.State;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessAction;

public interface ProcessingBusinessActionRepository extends MongoRepository<ProcessingBusinessAction, String> {
    ProcessingBusinessAction findByName(@Param("name") String name);

    ProcessingBusinessAction findByBusinessActionType(@Param("businessActionType") BusinessActionType businessActionType);

    ProcessingBusinessAction findFirstByStateOrderByPriorityAscCreatedDateAsc(@Param("state") State state);

    Stream<ProcessingBusinessAction> findByCreatedDateBeforeOrderByCreatedDateAsc(@Param("createdDate") LocalDateTime createdDate);

    Page<ProcessingBusinessAction> findByPersonalAccountId(@Param("accountId") String accountId, Pageable pageable);

    ProcessingBusinessAction findByIdAndPersonalAccountId(@Param("id") String id, @Param("accountId") String accountId);
}