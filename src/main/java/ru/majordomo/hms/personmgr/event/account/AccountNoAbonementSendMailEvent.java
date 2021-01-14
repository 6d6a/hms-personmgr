package ru.majordomo.hms.personmgr.event.account;

import org.springframework.context.ApplicationEvent;

public class AccountNoAbonementSendMailEvent extends ApplicationEvent {
    private Integer planCost;

    public AccountNoAbonementSendMailEvent(String accountId, Integer planCost) {
        super(accountId);
        this.planCost = planCost;
    }

    @Override
    public String getSource() {
        return (String) super.getSource();
    }
    public String getPlanCost() {
        return planCost.toString();
    }
}
