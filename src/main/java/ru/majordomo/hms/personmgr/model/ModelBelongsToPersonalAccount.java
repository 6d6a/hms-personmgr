package ru.majordomo.hms.personmgr.model;

import org.springframework.data.mongodb.core.index.Indexed;

import javax.validation.constraints.NotNull;

import ru.majordomo.hms.personmgr.validators.ObjectId;

/**
 * Класс наследуемый документами принадлежащими Аккаунту
 */
public class ModelBelongsToPersonalAccount extends BaseModel {
    @Indexed
    @NotNull
    @ObjectId(PersonalAccount.class)
    private String personalAccountId;

    public ModelBelongsToPersonalAccount() {
        super();
    }

    public String getPersonalAccountId() {
        return personalAccountId;
    }

    public void setPersonalAccountId(String personalAccountId) {
        this.personalAccountId = personalAccountId;
    }

    @Override
    public String toString() {
        return "ModelBelongsToPersonalAccount{" +
                "personalAccountId='" + personalAccountId + '\'' +
                "} " + super.toString();
    }
}
