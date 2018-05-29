package ru.majordomo.hms.personmgr.event.account;

import org.springframework.context.ApplicationEvent;

public class ProcessExpiringServiceAbonementsEvent extends ApplicationEvent {
    public ProcessExpiringServiceAbonementsEvent() {
        super("Process Expiring Service Abonements");
    }
}