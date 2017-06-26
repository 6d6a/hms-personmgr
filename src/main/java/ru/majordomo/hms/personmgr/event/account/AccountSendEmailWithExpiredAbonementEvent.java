package ru.majordomo.hms.personmgr.event.account;

import org.springframework.context.ApplicationEvent;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;

import java.util.Map;

public class AccountSendEmailWithExpiredAbonementEvent extends ApplicationEvent {

    public AccountSendEmailWithExpiredAbonementEvent(PersonalAccount source) {
        super(source);
    }

    @Override
    public PersonalAccount getSource() {
        return (PersonalAccount) super.getSource();
    }

}