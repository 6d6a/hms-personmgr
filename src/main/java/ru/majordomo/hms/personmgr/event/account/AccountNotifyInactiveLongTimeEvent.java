package ru.majordomo.hms.personmgr.event.account;

import org.springframework.context.ApplicationEvent;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;

import java.util.Map;

public class AccountNotifyInactiveLongTimeEvent extends ApplicationEvent {

    public AccountNotifyInactiveLongTimeEvent(PersonalAccount source) {
        super(source);
    }

    @Override
    public PersonalAccount getSource() {
        return (PersonalAccount) super.getSource();
    }
}