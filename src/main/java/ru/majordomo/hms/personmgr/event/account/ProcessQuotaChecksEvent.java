package ru.majordomo.hms.personmgr.event.account;

import org.springframework.context.ApplicationEvent;

public class ProcessQuotaChecksEvent extends ApplicationEvent {
    public ProcessQuotaChecksEvent() {
        super("Process Quota Checks");
    }
}