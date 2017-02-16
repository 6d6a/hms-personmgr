package ru.majordomo.hms.personmgr.event.mailManager;

import org.springframework.context.ApplicationEvent;

import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;

public class SendMailEvent extends ApplicationEvent {
    public SendMailEvent(SimpleServiceMessage source) {
        super(source);
    }

    @Override
    public SimpleServiceMessage getSource() {
        return (SimpleServiceMessage) super.getSource();
    }
}