package ru.majordomo.hms.personmgr.event.account;

import org.springframework.context.ApplicationEvent;
import ru.majordomo.hms.personmgr.model.PersonalAccount;

public class AccountDailyChargeEvent extends ApplicationEvent {
    public AccountDailyChargeEvent(PersonalAccount source) {
        super(source);
    }

    @Override
    public PersonalAccount getSource() {
        return (PersonalAccount) super.getSource();
    }
}
