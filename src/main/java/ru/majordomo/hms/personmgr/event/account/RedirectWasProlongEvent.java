package ru.majordomo.hms.personmgr.event.account;

import org.springframework.context.ApplicationEvent;

public class RedirectWasProlongEvent extends ApplicationEvent {
    private String domainName;

    public RedirectWasProlongEvent(String accountId, String domainName) {
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
