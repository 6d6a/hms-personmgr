package ru.majordomo.hms.personmgr.event.task.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import ru.majordomo.hms.personmgr.event.task.CleanFinishedTaskEvent;
import ru.majordomo.hms.personmgr.event.task.NewTasksExecuteEvent;
import ru.majordomo.hms.personmgr.event.task.TaskExecuteEvent;
import ru.majordomo.hms.personmgr.manager.TaskManager;
import ru.majordomo.hms.personmgr.model.task.State;
import ru.majordomo.hms.personmgr.model.task.Task;
import ru.majordomo.hms.personmgr.service.task.TaskProcessor;

import java.time.LocalDateTime;

@Component
@Slf4j
public class TaskListener {
    private final TaskProcessor taskProcessor;
    private final TaskManager taskManager;
    private final ApplicationEventPublisher publisher;
    private final static int CLEAN_FINISHED_AFTER_DAYS = 10;

    @Autowired
    public TaskListener(
            TaskProcessor taskProcessor,
            TaskManager taskManager,
            ApplicationEventPublisher publisher
    ) {
        this.taskProcessor = taskProcessor;
        this.taskManager = taskManager;
        this.publisher = publisher;
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void on(TaskExecuteEvent event) {
        log.info("We got TaskExecuteEvent");

        try {
            Task task = taskManager.getForProcessing(event.getSource());

            if (task == null) {
                log.error("task with id " + event.getSource() + " is null");
                return;
            }

            taskProcessor.execute(task);

            task.setState(State.FINISHED);

            taskManager.save(task);

        } catch (Exception e) {
            taskManager.setState(event.getSource(), State.ERROR);

            log.error(e.getMessage());

            e.printStackTrace();
        }
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void on(NewTasksExecuteEvent event) {
        log.info("We got AllTaskExecuteEvent");
        try {
            taskManager.findIdsForExecuting().forEach(taskId ->
                    publisher.publishEvent(
                            new TaskExecuteEvent(taskId)
                    )
            );
        } catch (Exception e) {
            log.error(e.getMessage());
            e.printStackTrace();
        }
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void cleanFinishedTask(CleanFinishedTaskEvent event) {
        log.info("We got AllTaskExecuteEvent");

        taskManager.cleanSucceedBefore(LocalDateTime.now().minusDays(CLEAN_FINISHED_AFTER_DAYS));
    }
}
