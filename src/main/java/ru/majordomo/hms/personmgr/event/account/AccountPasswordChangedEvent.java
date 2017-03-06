package ru.majordomo.hms.personmgr.event.account;

import org.springframework.context.ApplicationEvent;

import java.util.Map;

import ru.majordomo.hms.personmgr.model.PersonalAccount;

public class AccountPasswordChangedEvent extends ApplicationEvent {
    private Map<String, ?> params;

    public AccountPasswordChangedEvent(PersonalAccount source, Map<String, ?> params) {
        super(source);
        this.params = params;
    }

    @Override
    public PersonalAccount getSource() {
        return (PersonalAccount) super.getSource();
    }

    public Map<String, ?> getParams() {
        return params;
    }
}
