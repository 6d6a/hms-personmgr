package ru.majordomo.hms.personmgr.event.domain;

import org.springframework.context.ApplicationEvent;

import java.util.Map;

import ru.majordomo.hms.personmgr.model.PersonalAccount;

public class DomainNotEnoughMoneyToProlongEvent extends ApplicationEvent {
    private Map<String, ?> params;

    public DomainNotEnoughMoneyToProlongEvent(PersonalAccount source, Map<String, ?> params) {
        super(source);
        this.params = params;
    }

    public DomainNotEnoughMoneyToProlongEvent(PersonalAccount source) {
        super(source);
    }

    @Override
    public PersonalAccount getSource() {
        return (PersonalAccount) super.getSource();
    }

    public Map<String, ?> getParams() {
        return params;
    }
}
