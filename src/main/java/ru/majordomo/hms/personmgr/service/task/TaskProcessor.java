package ru.majordomo.hms.personmgr.service.task;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.model.task.Task;
import ru.majordomo.hms.personmgr.service.task.executor.Executor;
import ru.majordomo.hms.personmgr.service.task.executor.ExecutorFactory;

@Service
@Slf4j
public class TaskProcessor implements Executor<Task> {
    private final ExecutorFactory executorFactory;

    public TaskProcessor(
            ExecutorFactory executorFactory
    ) {
        this.executorFactory = executorFactory;
    }

    public void execute(Task task) {

        Executor executor = executorFactory.getExecutor(task.getClass());

        if (executor == null) {
            throw new ParameterValidationException("Невозможно обработать задачу " + task.toString());
        }

        executor.execute(task);
    }
}
