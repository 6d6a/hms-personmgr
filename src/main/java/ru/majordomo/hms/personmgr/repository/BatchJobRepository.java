package ru.majordomo.hms.personmgr.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDate;
import java.util.Optional;

import ru.majordomo.hms.personmgr.model.batch.BatchJob;

public interface BatchJobRepository extends MongoRepository<BatchJob, String> {
    BatchJob findFirstByRunDateAndTypeOrderByCreatedDesc(LocalDate runDate, BatchJob.Type type);
    Optional<BatchJob> findFirstByStateAndTypeOrderByCreatedDesc(BatchJob.State state, BatchJob.Type type);
}