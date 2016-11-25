package ru.majordomo.hms.personmgr.model.service;

import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import javax.validation.constraints.NotNull;

import ru.majordomo.hms.personmgr.common.AccountType;
import ru.majordomo.hms.personmgr.common.Costable;
import ru.majordomo.hms.personmgr.common.Limitable;
import ru.majordomo.hms.personmgr.common.Nameable;
import ru.majordomo.hms.personmgr.common.ServicePaymentType;
import ru.majordomo.hms.personmgr.common.Switchable;
import ru.majordomo.hms.personmgr.model.BaseModel;
import ru.majordomo.hms.personmgr.validators.ObjectIdMap;

@Document
public class PaymentService extends BaseModel implements Costable, Nameable, Switchable, Limitable {

    @NotNull
    @Indexed
    private ServicePaymentType paymentType;

    @NotNull
    private BigDecimal cost;

    @NotNull
    private int limit;

    @NotNull
    private String name;

    @NotNull
    private AccountType accountType;

    private boolean active;

    @Indexed(unique = true)
    private String oldId;

//    @NotNull
    @ObjectIdMap(value = PaymentService.class)
    private Map<String, Integer> servicesIdsWithLimits= new HashMap<>();

    public ServicePaymentType getPaymentType() {
        return paymentType;
    }

    public void setPaymentType(ServicePaymentType paymentType) {
        this.paymentType = paymentType;
    }

    @Override
    public BigDecimal getCost() {
        return this.cost;
    }

    @Override
    public int getLimit() {
        return this.limit;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void setCost(BigDecimal cost) {
        this.cost = cost;
    }

    @Override
    public void setLimit(int limit) {
        this.limit = limit;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public void switchOn() {
        this.active = true;
    }

    @Override
    public void switchOff() {
        this.active = false;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public void setAccountType(AccountType accountType) {
        this.accountType = accountType;
    }

    public Map<String, Integer> getServicesIdsWithLimits() {
        return servicesIdsWithLimits;
    }

    public void setServicesIdsWithLimits(Map<String, Integer> servicesIdsWithLimits) {
        this.servicesIdsWithLimits = servicesIdsWithLimits;
    }

    public String getOldId() {
        return oldId;
    }

    public void setOldId(String oldId) {
        this.oldId = oldId;
    }

    public PaymentService() {
    }

    @PersistenceConstructor
    public PaymentService(String id, ServicePaymentType paymentType, BigDecimal cost, int limit, String name, AccountType accountType, boolean active, Map<String, Integer> servicesIdsWithLimits, String oldId) {
        super();
        this.setId(id);
        this.paymentType = paymentType;
        this.cost = cost;
        this.limit = limit;
        this.name = name;
        this.accountType = accountType;
        this.active = active;
        this.servicesIdsWithLimits = servicesIdsWithLimits;
        this.oldId = oldId;
    }

    @Override
    public String toString() {
        return "PaymentService{" +
                "paymentType=" + paymentType +
                ", cost=" + cost +
                ", limit=" + limit +
                ", name='" + name + '\'' +
                ", accountType=" + accountType +
                ", active=" + active +
                ", oldId='" + oldId + '\'' +
                ", servicesIdsWithLimits=" + servicesIdsWithLimits +
                "} " + super.toString();
    }
}
