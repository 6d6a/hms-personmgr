package ru.majordomo.hms.personmgr.event.account;

import org.springframework.context.ApplicationEvent;

public class AccountBuyAbonement extends ApplicationEvent {
    private final String accountAbonementId;

    public AccountBuyAbonement(String personalAccountId, String accountAbonementId) {
        super(personalAccountId);
        this.accountAbonementId = accountAbonementId;
    }

    @Override
    public String getSource() {
        return (String) super.getSource();
    }

    public String getAccountAbonementId() {
        return accountAbonementId;
    }
}
