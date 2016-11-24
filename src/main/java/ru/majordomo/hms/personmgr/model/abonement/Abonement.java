package ru.majordomo.hms.personmgr.model.abonement;


import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.NotNull;

import ru.majordomo.hms.personmgr.common.AbonementType;
import ru.majordomo.hms.personmgr.common.FinService;
import ru.majordomo.hms.personmgr.model.BaseModel;

@Document
public class Abonement extends BaseModel {
    @NotNull
    private AbonementType type;
    @NotNull
    private String name;
    @NotNull
    private String period;
    @NotNull
    private String finServiceId;

    @Transient
    private FinService service;

    public Abonement() {
    }

    @PersistenceConstructor
    public Abonement(String id, AbonementType type, String name, String period, String finServiceId) {
        super();
        this.setId(id);
        this.type = type;
        this.name = name;
        this.period = period;
        this.finServiceId = finServiceId;
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

    public String getFinServiceId() {
        return finServiceId;
    }

    public void setFinServiceId(String finServiceId) {
        this.finServiceId = finServiceId;
    }

    public FinService getService() {
        return service;
    }

    public void setService(FinService service) {
        this.service = service;
    }

    @Override
    public String toString() {
        return "Abonement{" +
                "type=" + type +
                ", name='" + name + '\'' +
                ", period='" + period + '\'' +
                ", finServiceId='" + finServiceId + '\'' +
                ", service=" + service +
                "} " + super.toString();
    }
}
