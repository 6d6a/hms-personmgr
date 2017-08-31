package ru.majordomo.hms.personmgr.event.account;

import org.springframework.context.ApplicationEvent;

public class ProcessAccountDeactivatedSendMailEvent extends ApplicationEvent {
    public ProcessAccountDeactivatedSendMailEvent() {
        super("Process Account Deactivated");
    }
}