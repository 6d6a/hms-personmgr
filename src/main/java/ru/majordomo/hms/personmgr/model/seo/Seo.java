package ru.majordomo.hms.personmgr.model.seo;

import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.NotNull;

import ru.majordomo.hms.personmgr.common.FinService;
import ru.majordomo.hms.personmgr.common.SeoType;
import ru.majordomo.hms.personmgr.model.BaseModel;

@Document
public class Seo extends BaseModel {
    @NotNull
    private String name;
    @NotNull
    private SeoType type;

    @NotNull
    private String finServiceId;

    @Transient
    private FinService service;

    public Seo() {
    }

    @PersistenceConstructor
    public Seo(String id, String name, SeoType type, String finServiceId) {
        super();
        this.setId(id);
        this.name = name;
        this.type = type;
        this.finServiceId = finServiceId;
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
        return "Seo{" +
                "name='" + name + '\'' +
                ", type=" + type +
                ", finServiceId='" + finServiceId + '\'' +
                ", service=" + service +
                "} " + super.toString();
    }
}
