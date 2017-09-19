package ru.majordomo.hms.personmgr.event.account;

import org.springframework.context.ApplicationEvent;

import java.time.LocalDate;

public class ProcessErrorChargesEvent extends ApplicationEvent {
    private String batchJobId;
    private LocalDate chargeDate = LocalDate.now();

    public ProcessErrorChargesEvent(String batchJobId) {
        super("Process Error Charges");
        this.batchJobId = batchJobId;
    }

    public ProcessErrorChargesEvent(LocalDate chargeDate, String batchJobId) {
        super("Process Error Charges");
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