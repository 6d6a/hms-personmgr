package ru.majordomo.hms.personmgr.event.account;

import org.springframework.context.ApplicationEvent;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;

import java.util.Map;

public class AccountNotifyExpiredAbonementEvent extends ApplicationEvent {

    public AccountNotifyExpiredAbonementEvent(PersonalAccount source) {
        super(source);
    }

    @Override
    public PersonalAccount getSource() {
        return (PersonalAccount) super.getSource();
    }

}