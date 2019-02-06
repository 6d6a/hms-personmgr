package ru.majordomo.hms.personmgr.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessAction;

public interface ProcessingBusinessActionRepository extends MongoRepository<ProcessingBusinessAction, String> {
    ProcessingBusinessAction findByName(String name);

    Stream<ProcessingBusinessAction> findByCreatedDateBeforeOrderByCreatedDateAsc(LocalDateTime createdDate);

    Page<ProcessingBusinessAction> findByPersonalAccountId(String accountId, Pageable pageable);

    ProcessingBusinessAction findByIdAndPersonalAccountId(String id, String accountId);

    List<ProcessingBusinessAction> findAllByOperationId(String operationId);
}