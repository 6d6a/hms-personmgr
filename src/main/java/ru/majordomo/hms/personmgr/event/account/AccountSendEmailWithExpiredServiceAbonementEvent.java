package ru.majordomo.hms.personmgr.event.account;

import org.springframework.context.ApplicationEvent;

import java.math.BigDecimal;

public class AccountSendEmailWithExpiredServiceAbonementEvent extends ApplicationEvent {
    private final String serviceName;
    private final String dateFinish;
    private final String domains;
    private final BigDecimal balance;

    public AccountSendEmailWithExpiredServiceAbonementEvent(
            String accountId,
            String serviceName,
            String dateFinish,
            String domains,
            BigDecimal balance
    ) {
        super(accountId);
        this.serviceName = serviceName;
        this.dateFinish = dateFinish;
        this.domains = domains;
        this.balance = balance;
    }

    @Override
    public String getSource() {
        return (String) super.getSource();
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getDateFinish() {
        return dateFinish;
    }

    public String getDomains() {
        return domains;
    }

    public BigDecimal getBalance() {
        return balance;
    }
}