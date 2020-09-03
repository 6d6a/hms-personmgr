package ru.majordomo.hms.personmgr.model.domain;

import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import ru.majordomo.hms.personmgr.common.DomainCategory;
import ru.majordomo.hms.personmgr.model.BaseModel;
import ru.majordomo.hms.personmgr.model.service.PaymentService;
import ru.majordomo.hms.rc.user.resources.DomainRegistrar;

import javax.validation.constraints.NotNull;

import java.net.IDN;

/**
 * DomainTld
 */
@Document
public class DomainTld extends BaseModel {
    /** tld в формате punycode */
    @Indexed
    @NotNull
    private String tld;
    @Indexed
    @NotNull
    private DomainRegistrar registrar;
    @NotNull
    private byte registerYears;
    @NotNull
    private byte renewYears;
    @NotNull
    private short renewStartDays;
    @NotNull
    private short renewEndDays;
    @Indexed
    @NotNull
    private boolean active;
    @Indexed
    @NotNull
    private boolean variablePrice;
    @Indexed
    @NotNull
    private DomainCategory domainCategory;
    @Indexed
    @NotNull
    private short priority;

    @NotNull
    private String registrationServiceId;

    @NotNull
    private String renewServiceId;

    @Transient
    private PaymentService registrationService;

    @Transient
    private PaymentService renewService;

    public DomainTld() {
    }

    @PersistenceConstructor
    public DomainTld(String id, String tld, DomainRegistrar registrar, byte registerYears, byte renewYears, short renewStartDays, short renewEndDays, boolean active, boolean variablePrice, DomainCategory domainCategory, short priority, String registrationServiceId, String renewServiceId) {
        super();
        this.registrationServiceId = registrationServiceId;
        this.renewServiceId = renewServiceId;
        this.setId(id);
        this.tld = tld;
        this.registrar = registrar;
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

    public DomainRegistrar getRegistrar() {
        return registrar;
    }

    public void setRegistrar(DomainRegistrar registrar) {
        this.registrar = registrar;
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

    public String getRegistrationServiceId() {
        return registrationServiceId;
    }

    public void setRegistrationServiceId(String registrationServiceId) {
        this.registrationServiceId = registrationServiceId;
    }

    public String getRenewServiceId() {
        return renewServiceId;
    }

    public void setRenewServiceId(String renewServiceId) {
        this.renewServiceId = renewServiceId;
    }

    public PaymentService getRegistrationService() {
        return registrationService;
    }

    public void setRegistrationService(PaymentService registrationService) {
        this.registrationService = registrationService;
    }

    public PaymentService getRenewService() {
        return renewService;
    }

    public void setRenewService(PaymentService renewService) {
        this.renewService = renewService;
    }

    public String getEncodedTld() {
        return IDN.toUnicode(tld);
    }

    @Override
    public String toString() {
        return "DomainTld{" +
                "tld='" + tld + '\'' +
                ", registrar=" + registrar +
                ", registerYears=" + registerYears +
                ", renewYears=" + renewYears +
                ", renewStartDays=" + renewStartDays +
                ", renewEndDays=" + renewEndDays +
                ", active=" + active +
                ", variablePrice=" + variablePrice +
                ", domainCategory=" + domainCategory +
                ", priority=" + priority +
                ", registrationServiceId='" + registrationServiceId + '\'' +
                ", renewServiceId='" + renewServiceId + '\'' +
                ", registrationService=" + registrationService +
                ", renewService=" + renewService +
                "} " + super.toString();
    }
}
