package ru.majordomo.hms.personmgr.event.account;

import org.springframework.context.ApplicationEvent;
import ru.majordomo.hms.personmgr.model.PersonalAccount;

import java.util.Map;

public class AccountPromotionProcessByPaymentCreatedEvent extends ApplicationEvent {
    private Map<String, ?> params;

    public AccountPromotionProcessByPaymentCreatedEvent(PersonalAccount source, Map<String, ?> params) {
        super(source);
        this.params = params;
    }

    @Override
    public PersonalAccount getSource() {
        return (PersonalAccount) super.getSource();
    }

    public Map<String, ?> getParams() {
        return params;
    }
}
