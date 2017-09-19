package ru.majordomo.hms.personmgr.event.account;

import org.springframework.context.ApplicationEvent;

public class CleanBusinessActionsEvent extends ApplicationEvent {
    public CleanBusinessActionsEvent() {
        super("Clean Business Actions");
    }
}