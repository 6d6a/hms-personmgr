package ru.majordomo.hms.personmgr.model.plan;

import java.math.BigDecimal;

public class PlanChangeAgreement {
    private BigDecimal balance;
    private BigDecimal delta;
    private BigDecimal needToFeelBalance;
    private Boolean balanceChanges;

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public BigDecimal getDelta() {
        return delta;
    }

    public void setDelta(BigDecimal delta) {
        this.delta = delta;
    }

    public BigDecimal getNeedToFeelBalance() {
        return needToFeelBalance;
    }

    public void setNeedToFeelBalance(BigDecimal needToFeelBalance) {
        this.needToFeelBalance = needToFeelBalance;
    }

    public Boolean getBalanceChanges() {
        return balanceChanges;
    }

    public void setBalanceChanges(Boolean balanceChanges) {
        this.balanceChanges = balanceChanges;
    }

    @Override
    public String toString() {
        return "PlanChangeAgreement{" +
                "balance=" + balance +
                "delta=" + delta +
                "needToFeelBalance=" + needToFeelBalance +
                "balanceChanges=" + balanceChanges +
                "} " + super.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PlanChangeAgreement that = (PlanChangeAgreement) o;

        if (getBalance() != null ? !(getBalance().compareTo(that.getBalance()) == 0) : that.getBalance() != null) return false;
        if (getDelta() != null ? !(getDelta().compareTo(that.getDelta()) == 0) : that.getDelta() != null) return false;
        if (getNeedToFeelBalance() != null ? !(getNeedToFeelBalance().compareTo(that.getNeedToFeelBalance()) == 0) : that.getNeedToFeelBalance() != null)
            return false;
        return getBalanceChanges() != null ? getBalanceChanges().equals(that.getBalanceChanges()) : that.getBalanceChanges() == null;
    }
}
