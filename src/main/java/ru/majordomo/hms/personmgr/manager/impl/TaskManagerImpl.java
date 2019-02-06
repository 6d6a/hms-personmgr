package ru.majordomo.hms.personmgr.manager.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;
import ru.majordomo.hms.personmgr.dto.IdsContainer;
import ru.majordomo.hms.personmgr.manager.TaskManager;
import ru.majordomo.hms.personmgr.model.task.State;
import ru.majordomo.hms.personmgr.model.task.Task;
import ru.majordomo.hms.personmgr.repository.TaskRepository;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@Component
public class TaskManagerImpl implements TaskManager {
    private final MongoOperations mongoOperations;
    private final TaskRepository repository;

    @Autowired
    public TaskManagerImpl(
            MongoOperations mongoOperations,
            TaskRepository repository
    ) {
        this.mongoOperations = mongoOperations;
        this.repository = repository;
    }

    @Override
    public Task getForProcessing(String id) {
        return mongoOperations.findAndModify(
                new Query(
                        Criteria.where("id").is(id)
                                .and("state").is(State.NEW.name())
                ),
                new Update().set("state", State.PROCESSING).currentDate("updated"),
                Task.class
        );
    }

    @Override
    public Task save(Task task) {
        return repository.save(task);
    }

    @Override
    public void setState(String taskId, State state) {
        mongoOperations.updateFirst(
                new Query(Criteria.where("id").is(taskId)),
                new Update().set("state", state).currentDate("updated"),
                Task.class
        );
    }

    @Override
    public List<String> findIdsForExecuting() {
        List<IdsContainer> idsContainers = mongoOperations.aggregate(
                Aggregation.newAggregation(
                        Aggregation.match(
                                Criteria.where("state").is(State.NEW.name())
                                        .and("execAfter").lte(Date.from(LocalDateTime.now().toInstant(ZoneOffset.ofHours(3))))
                        ),
                        Aggregation.group().addToSet("id").as("ids")
                ),
                Task.class,
                IdsContainer.class
        ).getMappedResults();

        if (idsContainers == null || idsContainers.isEmpty() || idsContainers.get(0).getIds() == null) {
            return Collections.emptyList();
        } else {
            return idsContainers.get(0).getIds();
        }
    }

    @Override
    public void delete(String id) {
        repository.deleteById(id);
    }

    @Override
    public <S extends Task> List<S> findByExample(S task) {
        return repository.findAll(Example.of(task));
    }

    @Override
    public void cleanSucceedBefore(LocalDateTime updatedBefore) {
        repository.deleteAllByUpdatedBeforeAndState(updatedBefore, State.FINISHED);
    }
}
