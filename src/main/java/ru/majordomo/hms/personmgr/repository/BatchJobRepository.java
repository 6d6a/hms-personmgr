package ru.majordomo.hms.personmgr.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import ru.majordomo.hms.personmgr.model.batch.BatchJob;

public interface BatchJobRepository extends MongoRepository<BatchJob, String> {
}