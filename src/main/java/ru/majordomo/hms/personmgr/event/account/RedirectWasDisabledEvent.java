package ru.majordomo.hms.personmgr.event.account;

import org.springframework.context.ApplicationEvent;

public class RedirectWasDisabledEvent extends ApplicationEvent {
    private String domainName;

    public RedirectWasDisabledEvent(String accountId, String domainName) {
        super(accountId);

    }

    public String getDomainName() {
        return domainName;
    }

    @Override
    public String getSource() {
        return (String) super.getSource();
    }
}
