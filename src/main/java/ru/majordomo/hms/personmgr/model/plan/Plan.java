package ru.majordomo.hms.personmgr.model.plan;

import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.validation.constraints.NotNull;

import ru.majordomo.hms.personmgr.common.PlanSetting;
import ru.majordomo.hms.personmgr.common.AccountType;
import ru.majordomo.hms.personmgr.model.BaseModel;
import ru.majordomo.hms.personmgr.model.abonement.Abonement;
import ru.majordomo.hms.personmgr.model.service.PaymentService;
import ru.majordomo.hms.personmgr.validators.ObjectIdList;

import static ru.majordomo.hms.personmgr.common.PlanSetting.SSL_CERTIFICATE;

@Document
public class Plan extends BaseModel {
    @NotNull
    private String name;

    @NotNull
    private String internalName;

    @NotNull
    @Indexed
    private String serviceId;

    @NotNull
    @Indexed
    private String oldId;

    @NotNull
    private AccountType accountType;

    @NotNull
    @Indexed
    private boolean abonementOnly;

    @Indexed
    private boolean active;

    @NotNull
    private PlanProperties planProperties;

    private Map<PlanSetting, String> settings = new HashMap<>();

    @ObjectIdList(value = Abonement.class)
    private List<String> abonementIds = new ArrayList<>();

    private String smsServiceId;

    @Transient
    private PaymentService service;

    @Transient
    private List<Abonement> abonements = new ArrayList<>();

    @Transient
    private PaymentService smsService;

    public Plan() {
        super();
    }

    @PersistenceConstructor
    public Plan(String id, String name, String internalName, String serviceId, String oldId, AccountType accountType, boolean active, PlanProperties planProperties, List<String> abonementIds, boolean abonementOnly) {
        super();
        this.setId(id);
        this.serviceId = serviceId;
        this.oldId = oldId;
        this.name = name;
        this.internalName = internalName;
        this.accountType = accountType;
        this.active = active;
        this.planProperties = planProperties;
        this.abonementIds = abonementIds;
        this.abonementOnly = abonementOnly;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getInternalName() {
        return internalName;
    }

    public void setInternalName(String internalName) {
        this.internalName = internalName;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public void setAccountType(AccountType accountType) {
        this.accountType = accountType;
    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public PlanProperties getPlanProperties() {
        return planProperties;
    }

    public void setPlanProperties(PlanProperties planProperties) {
        this.planProperties = planProperties;
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

    public PaymentService getService() {
        return service;
    }

    public void setService(PaymentService service) {
        this.service = service;
    }

    public List<String> getAbonementIds() {
        return abonementIds;
    }

    public void setAbonementIds(List<String> abonementIds) {
        this.abonementIds = abonementIds;
    }

    public List<Abonement> getAbonements() {
        return abonements;
    }

    public void setAbonements(List<Abonement> abonements) {
        this.abonements = abonements;
    }

    public boolean isAbonementOnly() {
        return abonementOnly;
    }

    public void setAbonementOnly(boolean abonementOnly) {
        this.abonementOnly = abonementOnly;
    }

    public String getSmsServiceId() {
        return smsServiceId;
    }

    public void setSmsServiceId(String smsServiceId) {
        this.smsServiceId = smsServiceId;
    }

    public PaymentService getSmsService() {
        return smsService;
    }

    public void setSmsService(PaymentService smsService) {
        this.smsService = smsService;
    }

    public Map<PlanSetting, String> getSettings() {
        return settings;
    }

    public void setSettings(Map<PlanSetting, String> settings) {
        this.settings = settings;
    }

    public void setSetting(PlanSetting name, String setting) {
        this.settings.put(name, setting);
    }

    public void getSetting(String name) {
        this.settings.get(name);
    }

    private boolean getBooleanSettingByName(PlanSetting name) {
        return this.settings.get(name) != null ? Boolean.valueOf(this.settings.get(name)) : false;
    }

    private void setBooleanSettingByName(PlanSetting name, boolean value) {
        this.settings.put(name, String.valueOf(value));
    }

    public Boolean isSslCertificateAllowed() {
        return getBooleanSettingByName(SSL_CERTIFICATE);
    }

    public String getNotInternalAbonementId() {
        Abonement abonement = getNotInternalAbonement();
        return abonement != null ? abonement.getId() : null;
    }

    public Abonement getNotInternalAbonement() {
        for (Abonement abonement : this.getAbonements()) {
            if (!abonement.isInternal()) {
                return abonement;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return "Plan{" +
                "name='" + name + '\'' +
                ", internalName='" + internalName + '\'' +
                ", serviceId='" + serviceId + '\'' +
                ", oldId='" + oldId + '\'' +
                ", accountType=" + accountType +
                ", abonementOnly=" + abonementOnly +
                ", active=" + active +
                ", planProperties=" + planProperties +
                ", settings=" + settings +
                ", abonementIds=" + abonementIds +
                ", smsServiceId='" + smsServiceId + '\'' +
                ", service=" + service +
                ", abonements=" + abonements +
                ", smsService=" + smsService +
                "} " + super.toString();
    }
}
