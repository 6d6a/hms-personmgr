package ru.majordomo.hms.personmgr.event.task;

import org.springframework.context.ApplicationEvent;

public class SendLostDomainsInfoTaskEvent extends ApplicationEvent {
    public SendLostDomainsInfoTaskEvent() {
        super("");
    }
}
