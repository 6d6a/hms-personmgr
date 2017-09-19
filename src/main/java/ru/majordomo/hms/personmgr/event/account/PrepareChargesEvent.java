package ru.majordomo.hms.personmgr.event.account;

import org.springframework.context.ApplicationEvent;

import java.time.LocalDate;

public class PrepareChargesEvent extends ApplicationEvent {
    private String batchJobId;
    private LocalDate chargeDate = LocalDate.now();

    public PrepareChargesEvent(String batchJobId) {
        super("Prepare Charges");
        this.batchJobId = batchJobId;
    }

    public PrepareChargesEvent(LocalDate chargeDate, String batchJobId) {
        super("Prepare Charges");
        this.chargeDate = chargeDate;
        this.batchJobId = batchJobId;
    }

    public String getBatchJobId() {
        return batchJobId;
    }

    public LocalDate getChargeDate() {
        return chargeDate;
    }
}