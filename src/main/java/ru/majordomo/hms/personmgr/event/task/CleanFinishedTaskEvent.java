package ru.majordomo.hms.personmgr.event.task;

import org.springframework.context.ApplicationEvent;

public class CleanFinishedTaskEvent extends ApplicationEvent {
    public CleanFinishedTaskEvent() {
        super("");
    }
}
