package ru.majordomo.hms.personmgr.event.promocode;

import org.springframework.context.ApplicationEvent;

public class PromocodeImportEvent extends ApplicationEvent {
    public PromocodeImportEvent(String source) {
        super(source);
    }

    @Override
    public String getSource() {
        return (String) super.getSource();
    }
}
