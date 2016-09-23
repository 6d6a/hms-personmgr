package ru.majordomo.hms.personmgr.model.plan;

import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;

import javax.validation.constraints.NotNull;

import ru.majordomo.hms.personmgr.common.AccountType;
import ru.majordomo.hms.personmgr.model.BaseModel;

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
    private BigDecimal cost;

    @NotNull
    private PlanProperties planProperties;

    public Plan() {
        super();
    }

    @PersistenceConstructor
    public Plan(String id, String name, String internalName, String finServiceId, String oldId, AccountType accountType, boolean active, BigDecimal cost, PlanProperties planProperties) {
        this.finServiceId = finServiceId;
        this.oldId = oldId;
        this.setId(id);
        this.name = name;
        this.internalName = internalName;
        this.accountType = accountType;
        this.active = active;
        this.cost = cost;
        this.planProperties = planProperties;
    }

    @PersistenceConstructor
    public Plan(String name, String internalName, String finServiceId, String oldId, AccountType accountType, boolean active, BigDecimal cost, PlanProperties planProperties) {
        this.finServiceId = finServiceId;
        this.oldId = oldId;
        this.name = name;
        this.internalName = internalName;
        this.accountType = accountType;
        this.active = active;
        this.cost = cost;
        this.planProperties = planProperties;
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

    public BigDecimal getCost() {
        return cost;
    }

    public void setCost(BigDecimal cost) {
        this.cost = cost;
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

    @Override
    public String toString() {
        return "Plan{" +
                "name='" + name + '\'' +
                ", internalName='" + internalName + '\'' +
                ", finServiceId='" + finServiceId + '\'' +
                ", oldId='" + oldId + '\'' +
                ", accountType=" + accountType +
                ", active=" + active +
                ", cost=" + cost +
                ", planProperties=" + planProperties +
                "} " + super.toString();
    }
}
