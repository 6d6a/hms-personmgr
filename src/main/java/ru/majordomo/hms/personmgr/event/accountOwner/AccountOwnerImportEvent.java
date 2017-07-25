package ru.majordomo.hms.personmgr.event.accountOwner;

import org.springframework.context.ApplicationEvent;

public class AccountOwnerImportEvent extends ApplicationEvent {
    public AccountOwnerImportEvent(String source) {
        super(source);
    }

    @Override
    public String getSource() {
        return (String) super.getSource();
    }
}
