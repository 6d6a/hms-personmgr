package ru.majordomo.hms.personmgr.event.task;

import org.springframework.context.ApplicationEvent;

public class NewTasksExecuteEvent extends ApplicationEvent {
    public NewTasksExecuteEvent() {
        super("");
    }
}
