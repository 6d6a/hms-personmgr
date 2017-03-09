package ru.majordomo.hms.personmgr.event.accountPromocode;

import org.springframework.context.ApplicationEvent;

public class AccountPromocodeImportEvent extends ApplicationEvent {
    public AccountPromocodeImportEvent(String source) {
        super(source);
    }

    @Override
    public String getSource() {
        return (String) super.getSource();
    }
}
