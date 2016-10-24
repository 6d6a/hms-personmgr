package ru.majordomo.hms.personmgr.model.domain;

import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import ru.majordomo.hms.personmgr.model.ModelBelongsToPersonalAccount;

/**
 * AccountDomain
 */
@Document
public class AccountDomain extends ModelBelongsToPersonalAccount {
    private String resourceId;
    private String domainZoneId;
    private boolean autorenew;

    @Transient
    private DomainZone domainZone;

    public AccountDomain() {
    }

    @PersistenceConstructor
    public AccountDomain(String id, String personalAccountId, String resourceId, String domainZoneId, boolean autorenew) {
        super();
        this.autorenew = autorenew;
        this.setId(id);
        this.setPersonalAccountId(personalAccountId);
        this.resourceId = resourceId;
        this.domainZoneId = domainZoneId;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public String getDomainZoneId() {
        return domainZoneId;
    }

    public void setDomainZoneId(String domainZoneId) {
        this.domainZoneId = domainZoneId;
    }

    public DomainZone getDomainZone() {
        return domainZone;
    }

    public void setDomainZone(DomainZone domainZone) {
        this.domainZone = domainZone;
    }

    @Override
    public String toString() {
        return "AccountDomain{" +
                "resourceId='" + resourceId + '\'' +
                ", domainZoneId='" + domainZoneId + '\'' +
                ", autorenew=" + autorenew +
                ", domainZone=" + domainZone +
                "} " + super.toString();
    }

    public boolean isAutorenew() {
        return autorenew;
    }

    public void setAutorenew(boolean autorenew) {
        this.autorenew = autorenew;
    }

}
