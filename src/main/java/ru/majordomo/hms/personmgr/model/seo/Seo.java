package ru.majordomo.hms.personmgr.model.seo;

import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.NotNull;

import ru.majordomo.hms.personmgr.common.SeoType;
import ru.majordomo.hms.personmgr.model.BaseModel;
import ru.majordomo.hms.personmgr.model.service.PaymentService;

@Document
public class Seo extends BaseModel {
    @NotNull
    private String name;
    @NotNull
    private SeoType type;

    @NotNull
    private String serviceId;

    @Transient
    private PaymentService service;

    public Seo() {
    }

    @PersistenceConstructor
    public Seo(String id, String name, SeoType type, String serviceId) {
        super();
        this.setId(id);
        this.name = name;
        this.type = type;
        this.serviceId = serviceId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public SeoType getType() {
        return type;
    }

    public void setType(SeoType type) {
        this.type = type;
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
        return "Seo{" +
                "name='" + name + '\'' +
                ", type=" + type +
                ", serviceId='" + serviceId + '\'' +
                ", service=" + service +
                "} " + super.toString();
    }
}
