package ru.majordomo.hms.personmgr.event.account;

import org.springframework.context.ApplicationEvent;

public class AccountProcessDomainsAutoRenewEvent extends ApplicationEvent {
    public AccountProcessDomainsAutoRenewEvent(String accountId) {
        super(accountId);
    }

    @Override
    public String getSource() {
        return (String) super.getSource();
    }
}
