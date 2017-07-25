package ru.majordomo.hms.personmgr.event.promocode;

import org.springframework.context.ApplicationEvent;

public class PromocodeCleanEvent extends ApplicationEvent {
    public PromocodeCleanEvent(String source) {
        super(source);
    }

    @Override
    public String getSource() {
        return (String) super.getSource();
    }
}
