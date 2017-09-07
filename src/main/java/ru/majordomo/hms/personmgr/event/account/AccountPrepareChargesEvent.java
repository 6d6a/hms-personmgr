package ru.majordomo.hms.personmgr.event.account;

import org.springframework.context.ApplicationEvent;

import java.time.LocalDate;

public class AccountPrepareChargesEvent extends ApplicationEvent {
    private LocalDate chargeDate = LocalDate.now();

    public AccountPrepareChargesEvent(String source) {
        super(source);
    }

    public AccountPrepareChargesEvent(String source, LocalDate chargeDate) {
        super(source);
        this.chargeDate = chargeDate;
    }

    @Override
    public String getSource() {
        return (String) super.getSource();
    }

    public LocalDate getChargeDate() {
        return chargeDate;
    }

    public void setChargeDate(LocalDate chargeDate) {
        this.chargeDate = chargeDate;
    }
}
