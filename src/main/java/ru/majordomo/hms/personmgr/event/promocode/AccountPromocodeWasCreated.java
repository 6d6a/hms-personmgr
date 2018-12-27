package ru.majordomo.hms.personmgr.event.promocode;

import org.springframework.context.ApplicationEvent;
import ru.majordomo.hms.personmgr.model.promocode.AccountPromocode;

public class AccountPromocodeWasCreated extends ApplicationEvent {
    public AccountPromocodeWasCreated(AccountPromocode accountPromocode) {
        super(accountPromocode);
    }

    @Override
    public AccountPromocode getSource() {
        return (AccountPromocode) super.getSource();
    }
}
