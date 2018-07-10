package ru.majordomo.hms.personmgr.event.account;

import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;

public class AccountWasEnabled extends ApplicationEvent{
    private LocalDateTime deactivated;

    public AccountWasEnabled(String personalAccountId, LocalDateTime deactivated) {
        super(personalAccountId);
        this.deactivated = deactivated;
    }

    @Override
    public String getSource() {
        return (String) super.getSource();
    }

    public LocalDateTime getDeactivated() {
        return deactivated;
    }
}
