package ru.majordomo.hms.personmgr.event.account;

import org.springframework.context.ApplicationEvent;

public class AccountCreditExpiringWithHostingAbonementSendMailEvent extends ApplicationEvent {
    public AccountCreditExpiringWithHostingAbonementSendMailEvent(String accountId) {
        super(accountId);
    }

    @Override
    public String getSource() {
        return (String) super.getSource();
    }
}
