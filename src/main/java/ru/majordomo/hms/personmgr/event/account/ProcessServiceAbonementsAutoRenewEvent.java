package ru.majordomo.hms.personmgr.event.account;

import org.springframework.context.ApplicationEvent;

public class ProcessServiceAbonementsAutoRenewEvent extends ApplicationEvent {
    public ProcessServiceAbonementsAutoRenewEvent() {
        super("process Service Abonements Auto Renew");
    }
}