package ru.majordomo.hms.personmgr.event.account;

import org.springframework.context.ApplicationEvent;

public class PlanDailyDiagnosticEvent extends ApplicationEvent {
    public PlanDailyDiagnosticEvent() {
        super("Process plan daily diagnostic");
    }
}
