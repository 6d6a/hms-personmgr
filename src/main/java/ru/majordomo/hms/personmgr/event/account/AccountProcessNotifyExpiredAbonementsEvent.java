package ru.majordomo.hms.personmgr.event.account;

import org.springframework.context.ApplicationEvent;

public class AccountProcessNotifyExpiredAbonementsEvent extends ApplicationEvent {
    public AccountProcessNotifyExpiredAbonementsEvent(String accountId) {
        super(accountId);
    }

    @Override
    public String getSource() {
        return (String) super.getSource();
    }
}