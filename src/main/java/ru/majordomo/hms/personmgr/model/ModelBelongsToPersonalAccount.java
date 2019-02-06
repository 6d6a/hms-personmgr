package ru.majordomo.hms.personmgr.model;

import com.fasterxml.jackson.annotation.JsonView;
import com.querydsl.core.annotations.QueryTransient;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;

import javax.validation.constraints.NotNull;

import ru.majordomo.hms.personmgr.common.Views;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.validation.ObjectId;

/**
 * Класс наследуемый документами принадлежащими Аккаунту
 */
public class ModelBelongsToPersonalAccount extends BaseModel {
    @JsonView(Views.Internal.class)
    @Indexed
    @NotNull
    @ObjectId(PersonalAccount.class)
    private String personalAccountId;

    @JsonView(Views.Public.class)
    @Transient
    @QueryTransient
    private String personalAccountName;

    public ModelBelongsToPersonalAccount() {
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
        return "ModelBelongsToPersonalAccount{" +
                "personalAccountId='" + personalAccountId + '\'' +
                "} " + super.toString();
    }
}
