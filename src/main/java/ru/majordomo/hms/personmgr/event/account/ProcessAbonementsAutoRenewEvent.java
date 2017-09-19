package ru.majordomo.hms.personmgr.event.account;

import org.springframework.context.ApplicationEvent;

public class ProcessAbonementsAutoRenewEvent extends ApplicationEvent {
    public ProcessAbonementsAutoRenewEvent() {
        super("process Abonements Auto Renew");
    }
}