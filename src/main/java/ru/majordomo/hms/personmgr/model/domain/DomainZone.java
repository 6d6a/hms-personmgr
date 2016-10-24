package ru.majordomo.hms.personmgr.model.domain;

import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import ru.majordomo.hms.personmgr.common.DomainCategory;
import ru.majordomo.hms.personmgr.common.DomainRegistrator;
import ru.majordomo.hms.personmgr.model.BaseModel;

/**
 * DomainZone
 */
@Document
public class DomainZone extends BaseModel {
    @Indexed
    private String tld;
    @Indexed
    private DomainRegistrator domainRegistrator;
    private byte registerYears;
    private byte renewYears;
    private short renewStartDays;
    private short renewEndDays;
    @Indexed
    private boolean active;
    @Indexed
    private boolean variablePrice;
    @Indexed
    private DomainCategory domainCategory;
    @Indexed
    private short priority;

    public DomainZone() {
    }

    @PersistenceConstructor
    public DomainZone(String id, String tld, DomainRegistrator domainRegistrator, byte registerYears, byte renewYears, short renewStartDays, short renewEndDays, boolean active, boolean variablePrice, DomainCategory domainCategory, short priority) {
        super();
        this.setId(id);
        this.tld = tld;
        this.domainRegistrator = domainRegistrator;
        this.registerYears = registerYears;
        this.renewYears = renewYears;
        this.renewStartDays = renewStartDays;
        this.renewEndDays = renewEndDays;
        this.active = active;
        this.variablePrice = variablePrice;
        this.domainCategory = domainCategory;
        this.priority = priority;
    }

    public String getTld() {
        return tld;
    }

    public void setTld(String tld) {
        this.tld = tld;
    }

    public DomainRegistrator getDomainRegistrator() {
        return domainRegistrator;
    }

    public void setDomainRegistrator(DomainRegistrator domainRegistrator) {
        this.domainRegistrator = domainRegistrator;
    }

    public byte getRegisterYears() {
        return registerYears;
    }

    public void setRegisterYears(byte registerYears) {
        this.registerYears = registerYears;
    }

    public byte getRenewYears() {
        return renewYears;
    }

    public void setRenewYears(byte renewYears) {
        this.renewYears = renewYears;
    }

    public short getRenewStartDays() {
        return renewStartDays;
    }

    public void setRenewStartDays(short renewStartDays) {
        this.renewStartDays = renewStartDays;
    }

    public short getRenewEndDays() {
        return renewEndDays;
    }

    public void setRenewEndDays(short renewEndDays) {
        this.renewEndDays = renewEndDays;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isVariablePrice() {
        return variablePrice;
    }

    public void setVariablePrice(boolean variablePrice) {
        this.variablePrice = variablePrice;
    }

    public DomainCategory getDomainCategory() {
        return domainCategory;
    }

    public void setDomainCategory(DomainCategory domainCategory) {
        this.domainCategory = domainCategory;
    }

    public short getPriority() {
        return priority;
    }

    public void setPriority(short priority) {
        this.priority = priority;
    }

    @Override
    public String toString() {
        return "DomainZone{" +
                "tld='" + tld + '\'' +
                ", domainRegistrator='" + domainRegistrator + '\'' +
                ", registerYears=" + registerYears +
                ", renewYears=" + renewYears +
                ", renewStartDays=" + renewStartDays +
                ", renewEndDays=" + renewEndDays +
                ", active=" + active +
                ", variablePrice=" + variablePrice +
                ", domainCategory=" + domainCategory +
                ", priority=" + priority +
                ", domainRegistrator=" + domainRegistrator +
                "} " + super.toString();
    }
}
