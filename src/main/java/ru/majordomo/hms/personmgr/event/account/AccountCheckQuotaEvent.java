package ru.majordomo.hms.personmgr.event.account;

import org.springframework.context.ApplicationEvent;

import java.util.Map;

import ru.majordomo.hms.personmgr.model.PersonalAccount;

public class AccountCheckQuotaEvent extends ApplicationEvent {
    private Map<String, ?> params;

    public AccountCheckQuotaEvent(PersonalAccount source, Map<String, ?> params) {
        super(source);
        this.params = params;
    }

    public AccountCheckQuotaEvent(PersonalAccount source) {
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
