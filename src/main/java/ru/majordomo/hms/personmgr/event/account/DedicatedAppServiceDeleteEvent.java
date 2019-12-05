package ru.majordomo.hms.personmgr.event.account;

import org.springframework.context.ApplicationEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class DedicatedAppServiceDeleteEvent extends ApplicationEvent {
    @Nullable
    private String accountServiceId = null;

    public DedicatedAppServiceDeleteEvent(Object source, @Nonnull String accountServiceId) {
        super(source);
        this.accountServiceId = accountServiceId;
    }

    @Nullable
    public String getAccountServiceId() {
        return accountServiceId;
    }

    @Override
    public String getSource() {
        return (String) super.getSource();
    }
}
