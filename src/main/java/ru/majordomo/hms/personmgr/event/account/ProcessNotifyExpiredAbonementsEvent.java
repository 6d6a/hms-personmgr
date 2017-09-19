package ru.majordomo.hms.personmgr.event.account;

import org.springframework.context.ApplicationEvent;

public class ProcessNotifyExpiredAbonementsEvent extends ApplicationEvent {
    public ProcessNotifyExpiredAbonementsEvent() {
        super("Process Notify Expired Abonements");
    }
}