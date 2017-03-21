package ru.majordomo.hms.personmgr.model.abonement;

import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.annotation.Transient;
import ru.majordomo.hms.personmgr.model.ModelBelongsToPersonalAccount;
import ru.majordomo.hms.personmgr.validators.ObjectId;

import javax.validation.constraints.NotNull;

public class AccountAbonementPreorder extends ModelBelongsToPersonalAccount {
    @NotNull
    @ObjectId(Abonement.class)
    private String abonementId;

    @Transient
    private Abonement abonement;

    public AccountAbonementPreorder() {
    }

    @PersistenceConstructor
    public AccountAbonementPreorder(String id, String abonementId) {
        super();
        this.setId(id);
        this.abonementId = abonementId;
    }

    public String getAbonementId() {
        return abonementId;
    }

    public void setAbonementId(String abonementId) {
        this.abonementId = abonementId;
    }

    public Abonement getAbonement() {
        return abonement;
    }

    public void setAbonement(Abonement abonement) {
        this.abonement = abonement;
    }

    @Override
    public String toString() {
        return "AccountAbonementPreorder{" +
                "abonementId='" + abonementId + '\'' +
                ", abonement=" + abonement +
                "} " + super.toString();
    }
}
