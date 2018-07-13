package ru.majordomo.hms.personmgr.event.account;

import org.springframework.context.ApplicationEvent;

public class AccountServicesDisabledWithHostingAbonementSendMailEvent extends ApplicationEvent {
    public AccountServicesDisabledWithHostingAbonementSendMailEvent(String accountId) {
        super(accountId);
    }

    @Override
    public String getSource() {
        return (String) super.getSource();
    }
}
