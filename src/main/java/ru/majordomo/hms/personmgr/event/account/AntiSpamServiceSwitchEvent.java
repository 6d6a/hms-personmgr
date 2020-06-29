package ru.majordomo.hms.personmgr.event.account;

import org.springframework.context.ApplicationEvent;

public class AntiSpamServiceSwitchEvent extends ApplicationEvent {
    private final boolean enabled;

    public AntiSpamServiceSwitchEvent(String accountId, boolean enabled) {
        super(accountId);
        this.enabled = enabled;
    }

    public boolean getEnabled() {
        return enabled;
    }

    @Override
    public String getSource() {
        return (String) super.getSource();
    }
}
