package ru.majordomo.hms.personmgr.event.account;

import org.springframework.context.ApplicationEvent;

public class ProcessChargeEvent extends ApplicationEvent {
    private String batchJobId;

    public ProcessChargeEvent(String chargeRequestId, String batchJobId) {
        super(chargeRequestId);
        this.batchJobId = batchJobId;
    }

    public String getBatchJobId() {
        return batchJobId;
    }

    @Override
    public String getSource() {
        return (String) super.getSource();
    }
}