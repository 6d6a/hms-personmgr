package ru.majordomo.hms.personmgr.model.abonement;


import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.NotNull;

import ru.majordomo.hms.personmgr.common.AbonementType;
import ru.majordomo.hms.personmgr.model.BaseModel;
import ru.majordomo.hms.personmgr.model.service.PaymentService;
import ru.majordomo.hms.personmgr.validation.ObjectId;

@Document
public class Abonement extends BaseModel {
    @NotNull
    private AbonementType type;
    @NotNull
    private String name;
    @NotNull
    private String period;
    @NotNull
    @ObjectId(PaymentService.class)
    private String serviceId;

    @NotNull
    @Indexed
    private boolean internal;

    @Transient
    private PaymentService service;

    public Abonement() {
    }

    @PersistenceConstructor
    public Abonement(String id, AbonementType type, String name, String period, String serviceId, Boolean internal) {
        super();
        this.setId(id);
        this.type = type;
        this.name = name;
        this.period = period;
        this.serviceId = serviceId;
        this.internal = internal;
    }

    public AbonementType getType() {
        return type;
    }

    public void setType(AbonementType type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public PaymentService getService() {
        return service;
    }

    public void setService(PaymentService service) {
        this.service = service;
    }

    public boolean isInternal() {
        return internal;
    }

    public void setInternal(boolean internal) {
        this.internal = internal;
    }

    @Override
    public String toString() {
        return "Abonement{" +
                "type=" + type +
                ", name='" + name + '\'' +
                ", period='" + period + '\'' +
                ", serviceId='" + serviceId + '\'' +
                ", service=" + service +
                ", internal=" + internal +
                "} " + super.toString();
    }
}
