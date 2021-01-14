package ru.majordomo.hms.personmgr.event.account;

import org.springframework.context.ApplicationEvent;

public class ProcessAccountNoAbonementSendMailEvent extends ApplicationEvent {
    public ProcessAccountNoAbonementSendMailEvent() {
        super("Process Account No Abonement Send Mail");
    }
}