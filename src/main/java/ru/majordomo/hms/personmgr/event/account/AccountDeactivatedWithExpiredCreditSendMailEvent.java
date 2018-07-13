package ru.majordomo.hms.personmgr.event.account;

import org.springframework.context.ApplicationEvent;

public class AccountDeactivatedWithExpiredCreditSendMailEvent extends ApplicationEvent {
    public AccountDeactivatedWithExpiredCreditSendMailEvent(String accountId) {
        super(accountId);
    }

    @Override
    public String getSource() {
        return (String) super.getSource();
    }
}
