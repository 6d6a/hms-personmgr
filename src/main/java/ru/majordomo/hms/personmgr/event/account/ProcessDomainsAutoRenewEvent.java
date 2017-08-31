package ru.majordomo.hms.personmgr.event.account;

import org.springframework.context.ApplicationEvent;

public class ProcessDomainsAutoRenewEvent extends ApplicationEvent {
    public ProcessDomainsAutoRenewEvent() {
        super("Process Domains AutoRenew");
    }
}