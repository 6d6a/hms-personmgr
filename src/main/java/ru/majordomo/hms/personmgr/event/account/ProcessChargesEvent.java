package ru.majordomo.hms.personmgr.event.account;

import org.springframework.context.ApplicationEvent;

import java.time.LocalDate;

public class ProcessChargesEvent extends ApplicationEvent {
    private String batchJobId;
    private LocalDate chargeDate = LocalDate.now();

    public ProcessChargesEvent(String batchJobId) {
        super("Process Charges");
        this.batchJobId = batchJobId;
    }

    public ProcessChargesEvent(LocalDate chargeDate, String batchJobId) {
        super("Process Charges");
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