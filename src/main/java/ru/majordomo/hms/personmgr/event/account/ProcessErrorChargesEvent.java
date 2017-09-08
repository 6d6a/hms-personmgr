package ru.majordomo.hms.personmgr.event.account;

import org.springframework.context.ApplicationEvent;

import java.time.LocalDate;

public class ProcessErrorChargesEvent extends ApplicationEvent {
    private LocalDate chargeDate = LocalDate.now();

    public ProcessErrorChargesEvent() {
        super("Process Error Charges");
    }

    public ProcessErrorChargesEvent(LocalDate chargeDate) {
        super("Process Error Charges");
        this.chargeDate = chargeDate;
    }

    public LocalDate getChargeDate() {
        return chargeDate;
    }
}