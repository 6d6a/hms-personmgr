package ru.majordomo.hms.personmgr.manager.impl;

import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import ru.majordomo.hms.personmgr.exception.ResourceNotFoundException;
import ru.majordomo.hms.personmgr.manager.BatchJobManager;
import ru.majordomo.hms.personmgr.model.batch.BatchJob;
import ru.majordomo.hms.personmgr.repository.BatchJobRepository;

@Component
public class BatchJobManagerImpl implements BatchJobManager {
    private final BatchJobRepository repository;
    private final MongoOperations mongoOperations;

    public BatchJobManagerImpl(
            BatchJobRepository repository,
            MongoOperations mongoOperations
    ) {
        this.repository = repository;
        this.mongoOperations = mongoOperations;
    }

    @Override
    public boolean exists(String id) {
        return repository.existsById(id);
    }

    @Override
    public long count() {
        return repository.count();
    }

    @Override
    public void delete(String id) {
        repository.deleteById(id);
    }

    @Override
    public void delete(BatchJob batchJob) {
        repository.delete(batchJob);
    }

    @Override
    public void delete(Iterable<BatchJob> batchJobs) {
        repository.deleteAll(batchJobs);
    }

    @Override
    public void deleteAll() {
        repository.deleteAll();
    }

    @Override
    public BatchJob save(BatchJob batchJob) {
        return repository.save(setUpdated(batchJob));
    }

    @Override
    public List<BatchJob> save(Iterable<BatchJob> batchJobs) {
        return repository.saveAll(
                StreamSupport
                        .stream(batchJobs.spliterator(), false)
                        .map(this::setUpdated)
                        .collect(Collectors.toSet()));
    }

    @Override
    public BatchJob insert(BatchJob batchJob) {
        return repository.insert(setUpdated(setCreated(batchJob)));
    }

    @Override
    public List<BatchJob> insert(Iterable<BatchJob> batchJobs) {
        return repository.insert(
                StreamSupport
                        .stream(batchJobs.spliterator(), false)
                        .map(this::setCreated)
                        .map(this::setUpdated)
                        .collect(Collectors.toSet())
        );
    }

    @Override
    public BatchJob findOne(String id) {
        checkById(id);

        return repository.findById(id).orElse(null);
    }

    @Override
    public List<BatchJob> findAll() {
        return repository.findAll();
    }

    @Override
    public BatchJob findFirstByRunDateAndTypeOrderByCreatedDesc(LocalDate runDate, BatchJob.Type type) {
        return repository.findFirstByRunDateAndTypeOrderByCreatedDesc(runDate, type);
    }

    @Override
    public void setStateToProcessing(String id) {
        checkById(id);

        Query query = new Query(new Criteria("_id").is(id));
        Update update = new Update()
                .set("state", BatchJob.State.PROCESSING)
                .currentDate("updated");

        mongoOperations.updateFirst(query, update, BatchJob.class);
    }

    @Override
    public void setStateToFinished(String id) {
        checkById(id);

        Query query = new Query(new Criteria("_id").is(id));
        Update update = new Update()
                .set("state", BatchJob.State.FINISHED)
                .currentDate("updated");

        mongoOperations.updateFirst(query, update, BatchJob.class);
    }


    @Override
    public void setNeedToProcess(String id, int needToProcess) {
        checkById(id);

        Query query = new Query(new Criteria("_id").is(id));
        Update update = new Update()
                .set("needToProcess", needToProcess)
                .currentDate("updated");

        mongoOperations.updateFirst(query, update, BatchJob.class);
    }

    @Override
    public void setCount(String id, int count) {
        checkById(id);

        Query query = new Query(new Criteria("_id").is(id));
        Update update = new Update()
                .set("count", count)
                .currentDate("updated");

        mongoOperations.updateFirst(query, update, BatchJob.class);
    }

    @Override
    public void incrementNeedToProcess(String id) {
        incrementNeedToProcess(id, 1);
    }

    @Override
    public void incrementNeedToProcess(String id, int count) {
        checkById(id);

        Query query = new Query(new Criteria("_id").is(id));
        Update update = new Update()
                .inc("needToProcess", count)
                .currentDate("updated");

        mongoOperations.updateFirst(query, update, BatchJob.class);

        setStateToFinishedIfNeeded(id);
    }

    @Override
    public void incrementProcessed(String id) {
        incrementProcessed(id, 1);
    }

    @Override
    public void incrementProcessed(String id, int count) {
        checkById(id);

        Query query = new Query(new Criteria("_id").is(id));
        Update update = new Update()
                .inc("processed", count)
                .currentDate("updated");

        mongoOperations.updateFirst(query, update, BatchJob.class);

        setStateToFinishedIfNeeded(id);
    }

    @Override
    public void setStateToFinishedIfNeeded(String id) {
        BatchJob batchJob = findOne(id);

        if (batchJob.getProcessed() == batchJob.getNeedToProcess()) {
            setStateToFinished(id);
        }
    }

    private void checkById(String id) {
        if (!exists(id)) {
            throw new ResourceNotFoundException("BatchJob ?? id: " + id + " ???? ????????????");
        }
    }

    private BatchJob setCreated(BatchJob batchJob) {
        batchJob.setCreated(LocalDateTime.now());

        return batchJob;
    }

    private BatchJob setUpdated(BatchJob batchJob) {
        batchJob.setUpdated(LocalDateTime.now());

        return batchJob;
    }
}
