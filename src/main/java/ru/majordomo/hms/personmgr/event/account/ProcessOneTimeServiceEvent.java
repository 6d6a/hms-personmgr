package ru.majordomo.hms.personmgr.event.account;

import org.springframework.context.ApplicationEvent;

public class ProcessOneTimeServiceEvent extends ApplicationEvent {
    public ProcessOneTimeServiceEvent() {
        super("Process One Time Service");
    }
}