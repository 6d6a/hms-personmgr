package ru.majordomo.hms.personmgr.event.account;

import org.springframework.context.ApplicationEvent;

import java.util.Map;

import ru.majordomo.hms.personmgr.model.account.PersonalAccount;

public class AccountAppInstalledEvent extends ApplicationEvent {
    private Map<String, String> params;

    public AccountAppInstalledEvent(PersonalAccount source, Map<String, String> params) {
        super(source);
        this.params = params;
    }

    @Override
    public PersonalAccount getSource() {
        return (PersonalAccount) super.getSource();
    }

    public Map<String, String> getParams() {
        return params;
    }
}
