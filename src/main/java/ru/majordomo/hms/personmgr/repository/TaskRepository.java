package ru.majordomo.hms.personmgr.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import ru.majordomo.hms.personmgr.model.task.State;
import ru.majordomo.hms.personmgr.model.task.Task;

import java.time.LocalDateTime;

public interface TaskRepository extends MongoRepository<Task, String> {
    void deleteAllByUpdatedBeforeAndState(LocalDateTime updated, State state);
}
