package ru.majordomo.hms.personmgr.event.account;

import org.springframework.context.ApplicationEvent;
import ru.majordomo.hms.personmgr.model.PersonalAccount;

public class AccountSwitchByPaymentCreatedEvent extends ApplicationEvent {

    public AccountSwitchByPaymentCreatedEvent(PersonalAccount source) {
        super(source);
    }

    @Override
    public PersonalAccount getSource() {
        return (PersonalAccount) super.getSource();
    }
}
