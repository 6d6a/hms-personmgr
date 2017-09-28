package ru.majordomo.hms.personmgr.model.seo;

import org.hibernate.validator.constraints.NotBlank;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

import javax.validation.constraints.NotNull;

import ru.majordomo.hms.personmgr.model.ModelBelongsToPersonalAccount;
import ru.majordomo.hms.personmgr.validation.DomainName;
import ru.majordomo.hms.personmgr.validation.ObjectId;

@Document
public class AccountSeoOrder extends ModelBelongsToPersonalAccount {
    @NotNull
    private LocalDateTime created;

    @ObjectId(Seo.class)
    @NotNull
    private String seoId;

    @NotBlank
    @DomainName
    private String domainName;

    @Transient
    private Seo seo;

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

    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
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
                ", domainName='" + domainName + '\'' +
                ", seo=" + seo +
                "} " + super.toString();
    }
}
