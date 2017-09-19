package ru.majordomo.hms.personmgr.event.account;

import org.springframework.context.ApplicationEvent;

public class ProcessRecurrentsEvent extends ApplicationEvent {
    public ProcessRecurrentsEvent() {
        super("Process Recurrents");
    }
}