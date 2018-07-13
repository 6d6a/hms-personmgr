package ru.majordomo.hms.personmgr.event.account;

import org.springframework.context.ApplicationEvent;

public class AccountServicesExpiringSendMailEvent extends ApplicationEvent {
    private int remainingDays;

    public AccountServicesExpiringSendMailEvent(String accountId, int remainingDays) {
        super(accountId);
        this.remainingDays = remainingDays;
    }

    @Override
    public String getSource() {
        return (String) super.getSource();
    }

    public int getRemainingDays() {
        return remainingDays;
    }
}
