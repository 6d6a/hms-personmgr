package ru.majordomo.hms.personmgr.event.accountAbonement;

import org.springframework.context.ApplicationEvent;

public class AccountAbonementImportEvent extends ApplicationEvent {
    public AccountAbonementImportEvent(String source) {
        super(source);
    }

    @Override
    public String getSource() {
        return (String) super.getSource();
    }
}
