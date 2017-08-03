package ru.majordomo.hms.personmgr.event.accountPromotion;

import org.springframework.context.ApplicationEvent;

public class AccountPromotionImportEvent extends ApplicationEvent {
    public AccountPromotionImportEvent(String source) {
        super(source);
    }

    @Override
    public String getSource() {
        return (String) super.getSource();
    }
}
