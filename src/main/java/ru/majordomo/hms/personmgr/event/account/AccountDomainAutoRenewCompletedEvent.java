package ru.majordomo.hms.personmgr.event.account;

import org.springframework.context.ApplicationEvent;

import java.util.Map;

import ru.majordomo.hms.personmgr.model.account.PersonalAccount;

public class AccountDomainAutoRenewCompletedEvent extends ApplicationEvent {
    private Map<String, ?> params;

    public AccountDomainAutoRenewCompletedEvent(PersonalAccount source, Map<String, ?> params) {
        super(source);
        this.params = params;
    }

    public AccountDomainAutoRenewCompletedEvent(PersonalAccount source) {
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
