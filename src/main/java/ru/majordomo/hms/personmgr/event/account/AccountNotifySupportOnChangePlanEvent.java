package ru.majordomo.hms.personmgr.event.account;

import org.springframework.context.ApplicationEvent;

import ru.majordomo.hms.personmgr.model.PersonalAccount;

import java.util.Map;

public class AccountNotifySupportOnChangePlanEvent extends ApplicationEvent {

    public AccountNotifySupportOnChangePlanEvent(PersonalAccount source) {
        super(source);
    }

    @Override
    public PersonalAccount getSource() {
        return (PersonalAccount) super.getSource();
    }
}
