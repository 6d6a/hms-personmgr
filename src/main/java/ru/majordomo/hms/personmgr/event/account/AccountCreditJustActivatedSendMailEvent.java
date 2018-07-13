package ru.majordomo.hms.personmgr.event.account;

import org.springframework.context.ApplicationEvent;

public class AccountCreditJustActivatedSendMailEvent extends ApplicationEvent {
    public AccountCreditJustActivatedSendMailEvent(String accountId) {
        super(accountId);
    }

    @Override
    public String getSource() {
        return (String) super.getSource();
    }
}
