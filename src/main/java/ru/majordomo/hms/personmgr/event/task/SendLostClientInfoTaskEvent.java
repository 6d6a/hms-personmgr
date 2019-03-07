package ru.majordomo.hms.personmgr.event.task;

import org.springframework.context.ApplicationEvent;

public class SendLostClientInfoTaskEvent extends ApplicationEvent {
    public SendLostClientInfoTaskEvent() {
        super("");
    }
}
