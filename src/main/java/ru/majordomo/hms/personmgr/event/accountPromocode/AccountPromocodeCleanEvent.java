package ru.majordomo.hms.personmgr.event.accountPromocode;

import org.springframework.context.ApplicationEvent;

public class AccountPromocodeCleanEvent extends ApplicationEvent {
    public AccountPromocodeCleanEvent(String source) {
        super(source);
    }

    @Override
    public String getSource() {
        return (String) super.getSource();
    }
}
