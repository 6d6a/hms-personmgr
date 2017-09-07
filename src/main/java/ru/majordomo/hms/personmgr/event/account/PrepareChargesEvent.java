package ru.majordomo.hms.personmgr.event.account;

import org.springframework.context.ApplicationEvent;

import java.time.LocalDate;

public class PrepareChargesEvent extends ApplicationEvent {
    private LocalDate chargeDate = LocalDate.now();

    public PrepareChargesEvent() {
        super("Prepare Charges");
    }

    public PrepareChargesEvent(LocalDate chargeDate) {
        super("Prepare Charges");
        this.chargeDate = chargeDate;
    }

    public LocalDate getChargeDate() {
        return chargeDate;
    }
}