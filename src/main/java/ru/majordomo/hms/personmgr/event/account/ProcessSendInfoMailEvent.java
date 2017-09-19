package ru.majordomo.hms.personmgr.event.account;

import org.springframework.context.ApplicationEvent;

public class ProcessSendInfoMailEvent extends ApplicationEvent {
    public ProcessSendInfoMailEvent() {
        super("Process Send Info Mail");
    }
}