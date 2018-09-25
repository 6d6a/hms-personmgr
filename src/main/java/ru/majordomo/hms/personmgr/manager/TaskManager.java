package ru.majordomo.hms.personmgr.manager;

import ru.majordomo.hms.personmgr.model.task.State;
import ru.majordomo.hms.personmgr.model.task.Task;

import java.time.LocalDateTime;
import java.util.List;

public interface TaskManager {
    Task save(Task task);

    Task getForProcessing(String id);

    void setState(String id, State state);

    void cleanSucceedBefore(LocalDateTime updatedBefore);

    List<String> findIdsForExecuting();

    void delete(String id);

    <S extends Task> List<S> findByExample(S task);
}
