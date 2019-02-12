package ru.majordomo.hms.personmgr.manager;

import java.time.LocalDate;
import java.util.List;

import ru.majordomo.hms.personmgr.model.batch.BatchJob;

public interface BatchJobManager {
    boolean exists(String id);

    long count();

    void delete(String id);

    void delete(BatchJob batchJob);

    void delete(Iterable<BatchJob> batchJobs);

    void deleteAll();

    BatchJob save(BatchJob batchJob);

    List<BatchJob> save(Iterable<BatchJob> batchJobs);

    BatchJob insert(BatchJob batchJob);

    List<BatchJob> insert(Iterable<BatchJob> batchJobs);

    BatchJob findOne(String id);

    List<BatchJob> findAll();

    BatchJob findFirstByRunDateAndTypeOrderByCreatedDesc(LocalDate runDate, BatchJob.Type type);

    void setStateToProcessing(String id);

    void setStateToFinished(String id);

    void setNeedToProcess(String id, int needToProcess);

    void setCount(String id, int count);

    void incrementNeedToProcess(String id);

    void incrementNeedToProcess(String id, int count);

    void incrementProcessed(String id);

    void incrementProcessed(String id, int count);

    void setStateToFinishedIfNeeded(String id);
}
