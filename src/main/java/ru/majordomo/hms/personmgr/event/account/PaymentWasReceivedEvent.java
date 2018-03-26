package ru.majordomo.hms.personmgr.event.account;

import org.springframework.context.ApplicationEvent;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;

public class PaymentWasReceivedEvent extends ApplicationEvent {

    public PaymentWasReceivedEvent(SimpleServiceMessage message) {
        super(message);
    }

    @Override
    public SimpleServiceMessage getSource() {
        return (SimpleServiceMessage) super.getSource();
    }
}
