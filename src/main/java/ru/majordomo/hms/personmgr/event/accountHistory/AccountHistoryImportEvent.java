package ru.majordomo.hms.personmgr.event.accountHistory;

import org.springframework.context.ApplicationEvent;

public class AccountHistoryImportEvent extends ApplicationEvent {
    public AccountHistoryImportEvent(String source) {
        super(source);
    }

    @Override
    public String getSource() {
        return (String) super.getSource();
    }
}
