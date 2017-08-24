package ru.majordomo.hms.personmgr.event.account;

import org.springframework.context.ApplicationEvent;

import java.util.Map;

public class AccountDomainAutoRenewCompletedEvent extends ApplicationEvent {
    private Map<String, ?> params;

    public AccountDomainAutoRenewCompletedEvent(String source, Map<String, ?> params) {
        super(source);
        this.params = params;
    }

    public AccountDomainAutoRenewCompletedEvent(String source) {
        super(source);
    }

    @Override
    public String getSource() {
        return (String) super.getSource();
    }

    public Map<String, ?> getParams() {
        return params;
    }
}
