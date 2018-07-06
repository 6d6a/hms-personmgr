package ru.majordomo.hms.personmgr.event.account;

import org.springframework.context.ApplicationEvent;

public class ProcessResourceArchivesEvent extends ApplicationEvent {
    public ProcessResourceArchivesEvent() {
        super("Process ResourceArchives");
    }
}