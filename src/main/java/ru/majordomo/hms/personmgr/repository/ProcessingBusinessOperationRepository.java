package ru.majordomo.hms.personmgr.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;


import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import ru.majordomo.hms.personmgr.common.BusinessOperationType;
import ru.majordomo.hms.personmgr.common.State;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessOperation;

public interface ProcessingBusinessOperationRepository extends MongoRepository<ProcessingBusinessOperation, String>,
        QuerydslPredicateExecutor<ProcessingBusinessOperation> {

    Page<ProcessingBusinessOperation> findByPersonalAccountId(String accountId, Pageable pageable);

    ProcessingBusinessOperation findByIdAndPersonalAccountId(String id, String accountId);

    List<ProcessingBusinessOperation> findAllByPersonalAccountIdAndTypeAndStateIn(String personalAccountId, BusinessOperationType type, Set<State> states);

    List<ProcessingBusinessOperation> findAllByPersonalAccountIdAndTypeInAndStateIn(String personalAccountId, List<BusinessOperationType> type, Set<State> states);

    List<ProcessingBusinessOperation> findAllByPersonalAccountIdAndTypeAndStateInAndCreatedDateGreaterThanEqual(String personalAccountId, BusinessOperationType type, Set<State> states, LocalDateTime createdDate);

    boolean existsByPersonalAccountIdAndTypeAndStateIn(String personalAccountId, BusinessOperationType type, Set<State> states);
}