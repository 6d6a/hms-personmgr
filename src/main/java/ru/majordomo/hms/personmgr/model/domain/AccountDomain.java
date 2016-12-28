package ru.majordomo.hms.personmgr.model.domain;

import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.NotNull;

import ru.majordomo.hms.personmgr.model.ModelBelongsToPersonalAccount;
import ru.majordomo.hms.rc.user.resources.DomainRegistrar;

/**
 * AccountDomain
 */
@Document
public class AccountDomain extends ModelBelongsToPersonalAccount {
    private String resourceId;

    @Indexed
    private DomainRegistrar registrar;

    @NotNull
    @Indexed(unique = true)
    private String name;

    @NotNull
    @Indexed
    private boolean autorenew;

    @Transient
    private DomainTld domainTld;

    public AccountDomain() {
    }

    @PersistenceConstructor
    public AccountDomain(String id, String name, String personalAccountId, String resourceId, DomainRegistrar registrar, boolean autorenew) {
        super();
        this.autorenew = autorenew;
        this.name = name;
        this.setId(id);
        this.setPersonalAccountId(personalAccountId);
        this.resourceId = resourceId;
        this.registrar = registrar;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public DomainRegistrar getRegistrar() {
        return registrar;
    }

    public void setRegistrar(DomainRegistrar registrar) {
        this.registrar = registrar;
    }

    public DomainTld getDomainTld() {
        return domainTld;
    }

    public void setDomainTld(DomainTld domainTld) {
        this.domainTld = domainTld;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isAutorenew() {
        return autorenew;
    }

    public void setAutorenew(boolean autorenew) {
        this.autorenew = autorenew;
    }

    @Override
    public String toString() {
        return "AccountDomain{" +
                "resourceId='" + resourceId + '\'' +
                ", registrar=" + registrar +
                ", name='" + name + '\'' +
                ", autorenew=" + autorenew +
                ", domainTld=" + domainTld +
                "} " + super.toString();
    }
}
