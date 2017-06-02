package ru.majordomo.hms.personmgr.common;

import java.math.BigDecimal;

public class AvailabilityInfo {
    private String domainName = null;
    private Boolean free = false;
    private BigDecimal premiumPrice = null;

    public AvailabilityInfo(){}

    public AvailabilityInfo(String domainName, Boolean free) {
        this.domainName = domainName;
        this.free = free;
    }

    public AvailabilityInfo(String domainName, Boolean free, BigDecimal premiumPrice) {
        this.domainName = domainName;
        this.free = free;
        this.premiumPrice = premiumPrice;
    }

    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public Boolean getFree() {
        return free;
    }

    public void setFree(Boolean free) {
        this.free = free;
    }

    public BigDecimal getPremiumPrice() {
        return premiumPrice;
    }

    public void setPremiumPrice(BigDecimal premiumPrice) {
        this.premiumPrice = premiumPrice;
    }
}
