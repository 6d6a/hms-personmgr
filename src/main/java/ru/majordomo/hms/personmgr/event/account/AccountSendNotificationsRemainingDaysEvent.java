package ru.majordomo.hms.personmgr.event.account;

import org.springframework.context.ApplicationEvent;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;

import java.util.Map;

public class AccountSendNotificationsRemainingDaysEvent extends ApplicationEvent {
    private Map<String, Object> params;

    public AccountSendNotificationsRemainingDaysEvent(PersonalAccount source, Map<String, Object> params) {
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