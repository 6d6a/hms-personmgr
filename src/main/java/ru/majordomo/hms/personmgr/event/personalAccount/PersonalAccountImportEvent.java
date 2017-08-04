package ru.majordomo.hms.personmgr.event.personalAccount;

import org.springframework.context.ApplicationEvent;

public class PersonalAccountImportEvent extends ApplicationEvent {
    public PersonalAccountImportEvent(String source) {
        super(source);
    }

    @Override
    public String getSource() {
        return (String) super.getSource();
    }
}
