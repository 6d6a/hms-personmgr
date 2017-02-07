package ru.majordomo.hms.personmgr.model.abonement;


import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.NotNull;

import ru.majordomo.hms.personmgr.common.AbonementType;
import ru.majordomo.hms.personmgr.model.BaseModel;
import ru.majordomo.hms.personmgr.model.service.PaymentService;
import ru.majordomo.hms.personmgr.validators.ObjectId;

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

    @Transient
    private PaymentService service;

    public Abonement() {
    }

    @PersistenceConstructor
    public Abonement(String id, AbonementType type, String name, String period, String serviceId) {
        super();
        this.setId(id);
        this.type = type;
        this.name = name;
        this.period = period;
        this.serviceId = serviceId;
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

    @Override
    public String toString() {
        return "Abonement{" +
                "type=" + type +
                ", name='" + name + '\'' +
                ", period='" + period + '\'' +
                ", serviceId='" + serviceId + '\'' +
                ", service=" + service +
                "} " + super.toString();
    }
}