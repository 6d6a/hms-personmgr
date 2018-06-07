package ru.majordomo.hms.personmgr.event.account;

import org.springframework.context.ApplicationEvent;

public class DeferredPlanChangeEvent extends ApplicationEvent {
    public DeferredPlanChangeEvent() {
        super("DeferredPlanChangeEvent");
    }
}
