package ru.majordomo.hms.personmgr.model.abonement;


import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

import javax.validation.constraints.NotNull;

import ru.majordomo.hms.personmgr.model.ModelBelongsToPersonalAccount;
import ru.majordomo.hms.personmgr.validators.ObjectId;

@Document
public class AccountAbonement extends ModelBelongsToPersonalAccount {
    @NotNull
    @ObjectId(Abonement.class)
    private String abonementId;

    @NotNull
    private LocalDateTime created;

    private LocalDateTime expired;

    @NotNull
    @Indexed
    private boolean autorenew;

    @NotNull
    @Indexed
    private boolean preordered;

    @Transient
    private Abonement abonement;

    public AccountAbonement() {
    }

    @PersistenceConstructor
    public AccountAbonement(String id, String abonementId, LocalDateTime created, LocalDateTime expired, boolean autorenew, boolean preordered) {
        super();
        this.setId(id);
        this.abonementId = abonementId;
        this.created = created;
        this.expired = expired;
        this.autorenew = autorenew;
        this.preordered = preordered;
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

    public boolean isPreordered() {
        return preordered;
    }

    public void setPreordered(boolean preordered) {
        this.preordered = preordered;
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
                ", preordered=" + preordered +
                "} " + super.toString();
    }
}
