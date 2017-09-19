package ru.majordomo.hms.personmgr.event.account;

import org.springframework.context.ApplicationEvent;

public class ProcessNotifyInactiveLongTimeEvent extends ApplicationEvent {
    public ProcessNotifyInactiveLongTimeEvent() {
        super("Process Notify Inactive Long Time");
    }
}