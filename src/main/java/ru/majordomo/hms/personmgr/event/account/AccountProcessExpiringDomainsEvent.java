package ru.majordomo.hms.personmgr.event.account;

import org.springframework.context.ApplicationEvent;

public class AccountProcessExpiringDomainsEvent extends ApplicationEvent {
    public AccountProcessExpiringDomainsEvent(String accountId) {
        super(accountId);
    }

    @Override
    public String getSource() {
        return (String) super.getSource();
    }
}
