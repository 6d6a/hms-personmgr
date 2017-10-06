package ru.majordomo.hms.personmgr.event.accountStat;

import org.springframework.context.ApplicationEvent;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;

public class AccountStatDomainUpdateEvent  extends ApplicationEvent {

    public AccountStatDomainUpdateEvent(SimpleServiceMessage message) {
        super(message);
    }

    @Override
    public SimpleServiceMessage getSource() {
        return (SimpleServiceMessage) super.getSource();
    }

}
