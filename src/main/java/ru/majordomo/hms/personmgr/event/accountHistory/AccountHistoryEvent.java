package ru.majordomo.hms.personmgr.event.accountHistory;

import org.springframework.context.ApplicationEvent;

import java.util.Map;

import ru.majordomo.hms.personmgr.model.PersonalAccount;

public class AccountHistoryEvent extends ApplicationEvent {
    private Map<String, ?> params;

    public AccountHistoryEvent(PersonalAccount source, Map<String, ?> params) {
        super(source);
        this.params = params;
    }

    public AccountHistoryEvent(PersonalAccount source) {
        super(source);
    }

    @Override
    public PersonalAccount getSource() {
        return (PersonalAccount) super.getSource();
    }

    public Map<String, ?> getParams() {
        return params;
    }
}
