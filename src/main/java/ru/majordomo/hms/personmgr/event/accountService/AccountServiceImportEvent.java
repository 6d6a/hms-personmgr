package ru.majordomo.hms.personmgr.event.accountService;

import org.springframework.context.ApplicationEvent;

public class AccountServiceImportEvent extends ApplicationEvent {
    public AccountServiceImportEvent(String source) {
        super(source);
    }

    @Override
    public String getSource() {
        return (String) super.getSource();
    }
}
