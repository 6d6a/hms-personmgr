package ru.majordomo.hms.personmgr.event.account;

import org.springframework.context.ApplicationEvent;

public class ProcessChargesEvent extends ApplicationEvent {
    public ProcessChargesEvent() {
        super("Process Charges");
    }
}