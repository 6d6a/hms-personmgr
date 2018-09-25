package ru.majordomo.hms.personmgr.event.task;

import org.springframework.context.ApplicationEvent;

public class TaskExecuteEvent extends ApplicationEvent{
    public TaskExecuteEvent(String taskId) {
        super(taskId);
    }

    @Override
    public String getSource() {
        return (String) super.getSource();
    }
}
