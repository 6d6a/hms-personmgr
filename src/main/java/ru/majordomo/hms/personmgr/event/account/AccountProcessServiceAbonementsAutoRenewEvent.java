package ru.majordomo.hms.personmgr.event.account;

import org.springframework.context.ApplicationEvent;

public class AccountProcessServiceAbonementsAutoRenewEvent extends ApplicationEvent {
    public AccountProcessServiceAbonementsAutoRenewEvent(String accountId) {
        super(accountId);
    }

    @Override
    public String getSource() {
        return (String) super.getSource();
    }
}
