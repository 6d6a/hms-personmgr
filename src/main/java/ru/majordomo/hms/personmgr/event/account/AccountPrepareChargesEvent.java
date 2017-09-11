package ru.majordomo.hms.personmgr.event.account;

import org.springframework.context.ApplicationEvent;

import java.time.LocalDate;

public class AccountPrepareChargesEvent extends ApplicationEvent {
    private String batchJobId;
    private LocalDate chargeDate = LocalDate.now();

    public AccountPrepareChargesEvent(String source, LocalDate chargeDate, String batchJobId) {
        super(source);
        this.chargeDate = chargeDate;
        this.batchJobId = batchJobId;
    }

    @Override
    public String getSource() {
        return (String) super.getSource();
    }

    public String getBatchJobId() {
        return batchJobId;
    }

    public LocalDate getChargeDate() {
        return chargeDate;
    }

    public void setChargeDate(LocalDate chargeDate) {
        this.chargeDate = chargeDate;
    }
}
