package ru.majordomo.hms.personmgr.event.account;

import org.springframework.context.ApplicationEvent;

public class AccountProcessAbonementsAutoRenewEvent extends ApplicationEvent {
    public AccountProcessAbonementsAutoRenewEvent(String accountId) {
        super(accountId);
    }

    @Override
    public String getSource() {
        return (String) super.getSource();
    }
}
