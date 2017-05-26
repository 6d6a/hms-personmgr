package ru.majordomo.hms.personmgr.model.abonement;


import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

import javax.validation.constraints.NotNull;

import ru.majordomo.hms.personmgr.model.VersionedModelBelongsToPersonalAccount;
import ru.majordomo.hms.personmgr.validators.ObjectId;
import ru.majordomo.hms.personmgr.validators.UniquePersonalAccountIdModel;

@Document
@UniquePersonalAccountIdModel(AccountAbonement.class)
public class AccountAbonement extends VersionedModelBelongsToPersonalAccount {
    @NotNull
    @ObjectId(Abonement.class)
    private String abonementId;

    @NotNull
    private LocalDateTime created;

    private LocalDateTime expired;

    @NotNull
    @Indexed
    private boolean autorenew;

    @Transient
    private Abonement abonement;

    public AccountAbonement() {
    }

    @PersistenceConstructor
    public AccountAbonement(String id, String abonementId, LocalDateTime created, LocalDateTime expired, boolean autorenew) {
        super();
        this.setId(id);
        this.abonementId = abonementId;
        this.created = created;
        this.expired = expired;
        this.autorenew = autorenew;
    }

    public String getAbonementId() {
        return abonementId;
    }

    public void setAbonementId(String abonementId) {
        this.abonementId = abonementId;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public void setCreated(LocalDateTime created) {
        this.created = created;
    }

    public LocalDateTime getExpired() {
        return expired;
    }

    public void setExpired(LocalDateTime expired) {
        this.expired = expired;
    }

    public boolean isAutorenew() {
        return autorenew;
    }

    public void setAutorenew(boolean autorenew) {
        this.autorenew = autorenew;
    }

    public Abonement getAbonement() {
        return abonement;
    }

    public void setAbonement(Abonement abonement) {
        this.abonement = abonement;
    }

    @Override
    public String toString() {
        return "AccountAbonement{" +
                "abonementId='" + abonementId + '\'' +
                ", created=" + created +
                ", expired=" + expired +
                ", autorenew=" + autorenew +
                ", abonement=" + abonement +
                "} " + super.toString();
    }
}
