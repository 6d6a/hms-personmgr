package ru.majordomo.hms.personmgr.model.seo;

import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

import javax.validation.constraints.NotNull;

import ru.majordomo.hms.personmgr.model.ModelBelongsToPersonalAccount;
import ru.majordomo.hms.personmgr.validators.ObjectId;

@Document
public class AccountSeoOrder extends ModelBelongsToPersonalAccount {
    @NotNull
    private LocalDateTime created;

    @ObjectId(Seo.class)
    private String seoId;

    @NotNull
    private String webSiteId;

    @Transient
    private Seo seo;

    public AccountSeoOrder() {
    }

    @PersistenceConstructor
    public AccountSeoOrder(String id, String personalAccountId, LocalDateTime created, String seoId, String webSiteId) {
        super();
        this.setId(id);
        this.setPersonalAccountId(personalAccountId);
        this.created = created;
        this.seoId = seoId;
        this.webSiteId = webSiteId;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public void setCreated(LocalDateTime created) {
        this.created = created;
    }

    public String getSeoId() {
        return seoId;
    }

    public void setSeoId(String seoId) {
        this.seoId = seoId;
    }

    public String getWebSiteId() {
        return webSiteId;
    }

    public void setWebSiteId(String webSiteId) {
        this.webSiteId = webSiteId;
    }

    public Seo getSeo() {
        return seo;
    }

    public void setSeo(Seo seo) {
        this.seo = seo;
    }

    @Override
    public String toString() {
        return "AccountSeoOrder{" +
                "created=" + created +
                ", seoId='" + seoId + '\'' +
                ", webSiteId='" + webSiteId + '\'' +
                ", seo=" + seo +
                "} " + super.toString();
    }
}
