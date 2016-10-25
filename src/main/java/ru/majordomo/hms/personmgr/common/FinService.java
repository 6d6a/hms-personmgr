package ru.majordomo.hms.personmgr.common;

import java.math.BigDecimal;

/**
 * FinService
 */
public class FinService {
    private String id;
    private ServicePaymentType paymentType;
    private String name;
    private BigDecimal cost;
    private AccountType accountType;
    private int limit;
    private boolean active;
    private String oldId;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public ServicePaymentType getPaymentType() {
        return paymentType;
    }

    public void setPaymentType(ServicePaymentType paymentType) {
        this.paymentType = paymentType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getCost() {
        return cost;
    }

    public void setCost(BigDecimal cost) {
        this.cost = cost;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public void setAccountType(AccountType accountType) {
        this.accountType = accountType;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getOldId() {
        return oldId;
    }

    public void setOldId(String oldId) {
        this.oldId = oldId;
    }

    @Override
    public String toString() {
        return "FinService{" +
                "id='" + id + '\'' +
                ", paymentType=" + paymentType +
                ", name='" + name + '\'' +
                ", cost=" + cost +
                ", accountType=" + accountType +
                ", limit=" + limit +
                ", active=" + active +
                ", oldId='" + oldId + '\'' +
                '}';
    }
}
