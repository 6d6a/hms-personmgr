package ru.majordomo.hms.personmgr.event.account;

import org.springframework.context.ApplicationEvent;

public class ProcessChargeInactiveAccountEvent extends ApplicationEvent {
    public ProcessChargeInactiveAccountEvent() {
        super("ProcessChargeInactiveAccountEvent");
    }
}
