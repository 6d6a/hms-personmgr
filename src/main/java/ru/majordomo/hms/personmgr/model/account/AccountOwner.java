package ru.majordomo.hms.personmgr.model.account;

import javax.validation.constraints.NotBlank;
import org.hibernate.validator.group.GroupSequenceProvider;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.validation.annotation.Validated;

import ru.majordomo.hms.personmgr.common.Utils;
import ru.majordomo.hms.personmgr.model.VersionedModelBelongsToPersonalAccount;
import ru.majordomo.hms.personmgr.validation.UniquePersonalAccountIdModel;
import ru.majordomo.hms.personmgr.validation.groupSequenceProvider.AccountOwnerGroupSequenceProvider;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import java.util.List;

@Document
@UniquePersonalAccountIdModel(AccountOwner.class)
@GroupSequenceProvider(value = AccountOwnerGroupSequenceProvider.class)
@Validated
public class AccountOwner extends VersionedModelBelongsToPersonalAccount {
    public enum Type {
        INDIVIDUAL,
        COMPANY,
        BUDGET_COMPANY
    }

    @NotBlank(message = "ФИО/Наименование организации владельца аккаунта должно быть указано")
    private String name;

    @NotNull(message = "Должен быть указан тип владельца аккаунта")
    private Type type;

    @Valid
    @NotNull(message = "Контактные данные должны быть заполнены")
    private ContactInfo contactInfo;

    @Valid
    private PersonalInfo personalInfo;

    @Transient
    private String accountId;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public ContactInfo getContactInfo() {
        return contactInfo;
    }

    public boolean equalEmailAdressess(AccountOwner owner) {
        List<String> emails = owner.getContactInfo().getEmailAddresses();
        List<String> emailsThis = this.getContactInfo().getEmailAddresses();
        if (emails.size() != emailsThis.size()) { return false; }
        return emails.containsAll(emailsThis);
    }

    public void setContactInfo(ContactInfo contactInfo) {
        this.contactInfo = contactInfo;
    }

    public PersonalInfo getPersonalInfo() {
        return personalInfo;
    }

    public void setPersonalInfo(PersonalInfo personalInfo) {
        this.personalInfo = personalInfo;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getDiffMessage(AccountOwner owner){

        PersonalInfo personalInfo = getPersonalInfo() == null ? new PersonalInfo() : getPersonalInfo();
        PersonalInfo newPersonalInfo = owner.getPersonalInfo() == null ? new PersonalInfo() : owner.getPersonalInfo();
        ContactInfo contactInfo = getContactInfo() == null ? new ContactInfo() : getContactInfo();
        ContactInfo newContactInfo = owner.getContactInfo() == null ? new ContactInfo() : owner.getContactInfo();


        return Utils.joinStringsWithDelimeterExceptNullStrings(
                ", ",
                Utils.diffFieldsString("имя", getName(), owner.getName()),
                Utils.diffFieldsString("тип", getType(), owner.getType()),
                personalInfo.getDiffMessage(newPersonalInfo),
                contactInfo.getDiffMessage(newContactInfo)
        );
    }

    @Override
    public String toString() {
        return "AccountOwner{" +
                "name='" + name + '\'' +
                ", type=" + type +
                ", contactInfo=" + contactInfo +
                ", personalInfo=" + personalInfo +
                "} " + super.toString();
    }
}
