package ru.majordomo.hms.personmgr.event.account;

import org.springframework.context.ApplicationEvent;

public class AccountCreditExpiredWithHostingAbonementSendMailEvent extends ApplicationEvent {
    public AccountCreditExpiredWithHostingAbonementSendMailEvent(String accountId) {
        super(accountId);
    }

    @Override
    public String getSource() {
        return (String) super.getSource();
    }
}
