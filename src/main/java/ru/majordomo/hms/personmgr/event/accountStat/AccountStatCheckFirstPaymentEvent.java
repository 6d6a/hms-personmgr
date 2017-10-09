package ru.majordomo.hms.personmgr.event.accountStat;

import org.springframework.context.ApplicationEvent;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;

public class AccountStatCheckFirstPaymentEvent extends ApplicationEvent {

    public AccountStatCheckFirstPaymentEvent(SimpleServiceMessage message) {
        super(message);
    }

    @Override
    public SimpleServiceMessage getSource() {
        return (SimpleServiceMessage) super.getSource();
    }

}
