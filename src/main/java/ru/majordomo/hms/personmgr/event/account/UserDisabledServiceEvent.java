package ru.majordomo.hms.personmgr.event.account;

import org.springframework.context.ApplicationEvent;

public class UserDisabledServiceEvent extends ApplicationEvent {
    private String paymentServiceId;

    public UserDisabledServiceEvent(String personalAccountId, String paymentServiceId) {
        super(personalAccountId);
        this.paymentServiceId = paymentServiceId;
    }

    @Override
    public String getSource(){
        return (String) super.getSource();
    }

    public String getPaymentServiceId() {
        return paymentServiceId;
    }
}
