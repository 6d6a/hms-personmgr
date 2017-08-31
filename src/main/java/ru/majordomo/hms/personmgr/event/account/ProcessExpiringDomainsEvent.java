package ru.majordomo.hms.personmgr.event.account;

import org.springframework.context.ApplicationEvent;

public class ProcessExpiringDomainsEvent extends ApplicationEvent {
    public ProcessExpiringDomainsEvent() {
        super("Process Expiring Domains");
    }
}