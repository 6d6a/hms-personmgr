package ru.majordomo.hms.personmgr.event.account;

import org.springframework.context.ApplicationEvent;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;

import java.util.Map;

public class AccountOwnerChangeEmailEvent extends ApplicationEvent {
    private Map<String, Object> params;

    public AccountOwnerChangeEmailEvent(PersonalAccount source, Map<String, Object> params) {
        super(source);
        this.params = params;
    }

    @Override
    public PersonalAccount getSource() {
        return (PersonalAccount) super.getSource();
    }

    public Map<String, Object> getParams() {
        return params;
    }
}
