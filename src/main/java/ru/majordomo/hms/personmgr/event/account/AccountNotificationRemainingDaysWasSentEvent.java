package ru.majordomo.hms.personmgr.event.account;

import org.springframework.context.ApplicationEvent;

public class AccountNotificationRemainingDaysWasSentEvent extends ApplicationEvent {
    private String apiName;

    public AccountNotificationRemainingDaysWasSentEvent(
            String accountId,
            String apiName
    ) {
        super(accountId);
        this.apiName = apiName;
    }

    @Override
    public String getSource() {
        return (String) super.getSource();
    }

    public String getApiName() {
        return apiName;
    }
}