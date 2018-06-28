package ru.majordomo.hms.personmgr.event.account;

import org.springframework.context.ApplicationEvent;

public class ProcessDeleteDataInactiveAccountsEvent extends ApplicationEvent {
    public ProcessDeleteDataInactiveAccountsEvent() {
        super("ProcessDeleteDataInactiveAccountsEvent");
    }
}
