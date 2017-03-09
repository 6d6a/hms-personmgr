package ru.majordomo.hms.personmgr.event.accountPromocode;

import org.springframework.context.ApplicationEvent;

import ru.majordomo.hms.personmgr.model.promocode.AccountPromocode;

public class AccountPromocodeCreateEvent extends ApplicationEvent {
    public AccountPromocodeCreateEvent(AccountPromocode source) {
        super(source);
    }

    @Override
    public AccountPromocode getSource() {
        return (AccountPromocode) super.getSource();
    }
}
