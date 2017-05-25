package ru.majordomo.hms.personmgr.model;

import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;

import javax.validation.constraints.NotNull;

import ru.majordomo.hms.personmgr.validators.ObjectId;

/**
 * Класс наследуемый документами принадлежащими Аккаунту + Versioned
 */
public class VersionedModelBelongsToPersonalAccount extends VersionedModel {
    @Indexed
    @NotNull
    @ObjectId(PersonalAccount.class)
    private String personalAccountId;

    @Transient
    private String personalAccountName;

    public VersionedModelBelongsToPersonalAccount() {
        super();
    }

    public String getPersonalAccountId() {
        return personalAccountId;
    }

    public void setPersonalAccountId(String personalAccountId) {
        this.personalAccountId = personalAccountId;
    }

    public String getPersonalAccountName() {
        return personalAccountName;
    }

    public void setPersonalAccountName(String personalAccountName) {
        this.personalAccountName = personalAccountName;
    }

    @Override
    public String toString() {
        return "VersionedModelBelongsToPersonalAccount{" +
                "personalAccountId='" + personalAccountId + '\'' +
                "} " + super.toString();
    }
}
