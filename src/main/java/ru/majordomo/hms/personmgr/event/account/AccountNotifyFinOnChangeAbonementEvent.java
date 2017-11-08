package ru.majordomo.hms.personmgr.event.account;

import org.springframework.context.ApplicationEvent;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;

public class AccountNotifyFinOnChangeAbonementEvent extends ApplicationEvent{

    public AccountNotifyFinOnChangeAbonementEvent(PersonalAccount account) {
        super(account);
    }

    @Override
    public PersonalAccount getSource(){
        return (PersonalAccount) super.getSource();
    }
}
