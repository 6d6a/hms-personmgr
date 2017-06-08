package ru.majordomo.hms.personmgr.event.webSite;

import org.springframework.context.ApplicationEvent;

import java.util.Map;

import ru.majordomo.hms.personmgr.model.account.PersonalAccount;

public class WebSiteCreatedEvent extends ApplicationEvent {
    private Map<String, ?> params;

    public WebSiteCreatedEvent(PersonalAccount source, Map<String, ?> params) {
        super(source);
        this.params = params;
    }

    @Override
    public PersonalAccount getSource() {
        return (PersonalAccount) super.getSource();
    }

    public Map<String, ?> getParams() {
        return params;
    }
}
