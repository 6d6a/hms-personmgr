package ru.majordomo.hms.personmgr.event.account;

import org.springframework.context.ApplicationEvent;

public class ProcessNotifyExpiringBitrixLicenseEvent extends ApplicationEvent{

    public ProcessNotifyExpiringBitrixLicenseEvent() {
        super("Expiring bitrix license notify");
    }
}
