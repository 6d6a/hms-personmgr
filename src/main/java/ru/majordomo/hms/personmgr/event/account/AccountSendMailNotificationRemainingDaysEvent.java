package ru.majordomo.hms.personmgr.event.account;

import org.springframework.context.ApplicationEvent;

import java.math.BigDecimal;

public class AccountSendMailNotificationRemainingDaysEvent extends ApplicationEvent {
    private int remainingDays;
    private int remainingCreditDays;
    private boolean hasActiveAbonement;
    private boolean hasActiveCredit;
    private boolean balanceIsPositive;
    private BigDecimal balance;

    public AccountSendMailNotificationRemainingDaysEvent(
            String accountId,
            int remainingDays,
            int remainingCreditDays,
            boolean hasActiveAbonement,
            boolean hasActiveCredit,
            boolean balanceIsPositive,
            BigDecimal balance
    ) {
        super(accountId);
        this.remainingDays = remainingDays;
        this.remainingCreditDays = remainingCreditDays;
        this.hasActiveAbonement = hasActiveAbonement;
        this.hasActiveCredit = hasActiveCredit;
        this.balanceIsPositive = balanceIsPositive;
        this.balance = balance;
    }

    @Override
    public String getSource() {
        return (String) super.getSource();
    }

    public int getRemainingDays() {
        return remainingDays;
    }

    public int getRemainingCreditDays() {
        return remainingCreditDays;
    }

    public boolean isHasActiveAbonement() {
        return hasActiveAbonement;
    }

    public boolean isHasActiveCredit() {
        return hasActiveCredit;
    }

    public boolean isBalanceIsPositive() {
        return balanceIsPositive;
    }

    public BigDecimal getBalance() {
        return balance;
    }
}