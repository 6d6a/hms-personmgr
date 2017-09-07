package ru.majordomo.hms.personmgr.event.account;

import org.springframework.context.ApplicationEvent;

import java.time.LocalDate;

public class ProcessChargesEvent extends ApplicationEvent {
    private LocalDate chargeDate = LocalDate.now();

    public ProcessChargesEvent() {
        super("Process Charges");
    }

    public ProcessChargesEvent(LocalDate chargeDate) {
        super("Process Charges");
        this.chargeDate = chargeDate;
    }

    public LocalDate getChargeDate() {
        return chargeDate;
    }
}