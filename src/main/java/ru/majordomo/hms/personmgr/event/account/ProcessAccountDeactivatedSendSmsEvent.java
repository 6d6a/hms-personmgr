package ru.majordomo.hms.personmgr.event.account;

import org.springframework.context.ApplicationEvent;

public class ProcessAccountDeactivatedSendSmsEvent extends ApplicationEvent {
    public ProcessAccountDeactivatedSendSmsEvent() {
        super("Process Account Deactivated Send Sms");
    }
}