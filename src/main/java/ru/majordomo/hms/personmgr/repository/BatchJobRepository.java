package ru.majordomo.hms.personmgr.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDate;

import ru.majordomo.hms.personmgr.model.batch.BatchJob;

public interface BatchJobRepository extends MongoRepository<BatchJob, String> {
    BatchJob findByRunDateAndTypeOrderByCreatedAsc(LocalDate runDate, BatchJob.Type type);
}