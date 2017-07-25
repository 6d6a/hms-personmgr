package ru.majordomo.hms.personmgr.event.personalAccount;

import org.springframework.context.ApplicationEvent;

public class PersonalAccountNotificationImportEvent extends ApplicationEvent {
    public PersonalAccountNotificationImportEvent(String source) {
        super(source);
    }

    @Override
    public String getSource() {
        return (String) super.getSource();
    }
}
