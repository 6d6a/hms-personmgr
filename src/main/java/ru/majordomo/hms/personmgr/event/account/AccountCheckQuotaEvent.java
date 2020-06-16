package ru.majordomo.hms.personmgr.event.account;

import org.springframework.context.ApplicationEvent;

import java.util.Optional;

public class AccountCheckQuotaEvent extends ApplicationEvent {

    private String batchJobId;

    public AccountCheckQuotaEvent(String accountId, String batchJobId) {
        super(accountId);
        this.batchJobId = batchJobId;
    }

    public AccountCheckQuotaEvent(String accountId) {
        super(accountId);
    }

    @Override
    public String getSource() {
        return (String) super.getSource();
    }

    public Optional<String> getBatchJobId() {
        return Optional.ofNullable(this.batchJobId);
    }
}
