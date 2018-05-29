package ru.majordomo.hms.personmgr.event.account;

import org.springframework.context.ApplicationEvent;

public class AccountProcessExpiringServiceAbonementsEvent extends ApplicationEvent {
    public AccountProcessExpiringServiceAbonementsEvent(String accountId) {
        super(accountId);
    }

    @Override
    public String getSource() {
        return (String) super.getSource();
    }
}
