package ru.majordomo.hms.personmgr.model.plan;

import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

import javax.validation.constraints.NotNull;

import ru.majordomo.hms.personmgr.common.AccountType;
import ru.majordomo.hms.personmgr.model.BaseModel;
import ru.majordomo.hms.personmgr.model.abonement.Abonement;
import ru.majordomo.hms.personmgr.model.service.PaymentService;
import ru.majordomo.hms.personmgr.validators.ObjectIdList;

/**
 * Plan
 */
@Document
public class Plan extends BaseModel {
    @NotNull
    private String name;

    @NotNull
    private String internalName;

    @NotNull
    private String finServiceId;

    @NotNull
    private String oldId;

    @NotNull
    private AccountType accountType;

    @Indexed
    private boolean active;

    @NotNull
    private PlanProperties planProperties;

    @ObjectIdList(value = Abonement.class)
    private List<String> abonementIds = new ArrayList<>();

    @Transient
    private PaymentService service;

    @Transient
    private List<Abonement> abonements = new ArrayList<>();

    public Plan() {
        super();
    }

    @PersistenceConstructor
    public Plan(String id, String name, String internalName, String finServiceId, String oldId, AccountType accountType, boolean active, PlanProperties planProperties, List<String> abonementIds) {
        super();
        this.setId(id);
        this.finServiceId = finServiceId;
        this.oldId = oldId;
        this.name = name;
        this.internalName = internalName;
        this.accountType = accountType;
        this.active = active;
        this.planProperties = planProperties;
        this.abonementIds = abonementIds;
    }

    public Plan(String name, String internalName, String finServiceId, String oldId, AccountType accountType, boolean active, PlanProperties planProperties, List<String> abonementIds) {
        super();
        this.finServiceId = finServiceId;
        this.oldId = oldId;
        this.name = name;
        this.internalName = internalName;
        this.accountType = accountType;
        this.active = active;
        this.planProperties = planProperties;
        this.abonementIds = abonementIds;
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

    public String getFinServiceId() {
        return finServiceId;
    }

    public void setFinServiceId(String finServiceId) {
        this.finServiceId = finServiceId;
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

    @Override
    public String toString() {
        return "Plan{" +
                "name='" + name + '\'' +
                ", internalName='" + internalName + '\'' +
                ", finServiceId='" + finServiceId + '\'' +
                ", oldId='" + oldId + '\'' +
                ", accountType=" + accountType +
                ", active=" + active +
                ", planProperties=" + planProperties +
                ", abonementIds=" + abonementIds +
                ", service=" + service +
                ", abonements=" + abonements +
                "} " + super.toString();
    }
}
