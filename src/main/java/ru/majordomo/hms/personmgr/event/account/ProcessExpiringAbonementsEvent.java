package ru.majordomo.hms.personmgr.event.account;

import org.springframework.context.ApplicationEvent;

public class ProcessExpiringAbonementsEvent extends ApplicationEvent {
    public ProcessExpiringAbonementsEvent() {
        super("Process Expiring Abonements");
    }
}