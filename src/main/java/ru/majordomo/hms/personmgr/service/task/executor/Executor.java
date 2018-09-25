package ru.majordomo.hms.personmgr.service.task.executor;

import ru.majordomo.hms.personmgr.model.task.Task;

public interface Executor<T extends Task> {
    void execute(T task);
}
