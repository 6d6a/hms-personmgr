package ru.majordomo.hms.personmgr.event.account;

import org.springframework.context.ApplicationEvent;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;

import java.util.Map;

public class NewAuthNotifyEvent extends ApplicationEvent {
    private Map<String, ?> params;

    public NewAuthNotifyEvent(PersonalAccount source, Map<String, ?> params) {
        super(source);
        this.params = params;
    }

    @Override
    public PersonalAccount getSource() {
        return (PersonalAccount) super.getSource();
    }

    public Map<String, String> getParams() {
        return (Map<String, String>) params;
    }
}
