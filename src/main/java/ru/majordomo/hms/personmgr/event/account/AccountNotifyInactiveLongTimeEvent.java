package ru.majordomo.hms.personmgr.event.account;

import org.springframework.context.ApplicationEvent;

public class AccountNotifyInactiveLongTimeEvent extends ApplicationEvent {
    public AccountNotifyInactiveLongTimeEvent(String accountId) {
        super(accountId);
    }

    @Override
    public String getSource() {
        return (String) super.getSource();
    }
}