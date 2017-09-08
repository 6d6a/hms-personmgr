package ru.majordomo.hms.personmgr.event.account;

import org.springframework.context.ApplicationEvent;

import java.time.LocalDate;

import ru.majordomo.hms.personmgr.model.charge.ChargeRequest;

public class ProcessChargeEvent extends ApplicationEvent {
    public ProcessChargeEvent(ChargeRequest chargeRequest) {
        super(chargeRequest);
    }

    @Override
    public ChargeRequest getSource() {
        return (ChargeRequest) super.getSource();
    }
}