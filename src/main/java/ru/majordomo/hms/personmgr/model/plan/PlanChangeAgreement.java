package ru.majordomo.hms.personmgr.model.plan;

import java.math.BigDecimal;

public class PlanChangeAgreement {
    private BigDecimal balance;
    private BigDecimal delta;
    private BigDecimal needToFeelBalance;

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

    @Override
    public String toString() {
        return "PlanChangeAgreement{" +
                "balance=" + balance +
                "delta=" + delta +
                "needToFeelBalance=" + needToFeelBalance +
                "} " + super.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PlanChangeAgreement that = (PlanChangeAgreement) o;

        if (getBalance() != null ? !getBalance().equals(that.getBalance()) : that.getBalance() != null) return false;
        if (getDelta() != null ? !getDelta().equals(that.getDelta()) : that.getDelta() != null) return false;
        return getNeedToFeelBalance() != null ? getNeedToFeelBalance().equals(that.getNeedToFeelBalance()) : that.getNeedToFeelBalance() == null;
    }
}
