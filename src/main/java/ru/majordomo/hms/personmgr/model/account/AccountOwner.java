package ru.majordomo.hms.personmgr.model.account;

import org.hibernate.validator.constraints.NotBlank;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.data.mongodb.core.mapping.Document;

import ru.majordomo.hms.personmgr.model.VersionedModelBelongsToPersonalAccount;
import ru.majordomo.hms.personmgr.validation.UniquePersonalAccountIdModel;
import ru.majordomo.hms.rc.user.resources.Person;
import ru.majordomo.hms.rc.user.resources.validation.ValidEmail;
import ru.majordomo.hms.rc.user.resources.validation.ValidPhone;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

@Document
@UniquePersonalAccountIdModel(AccountOwner.class)
public class AccountOwner extends VersionedModelBelongsToPersonalAccount {
    public enum Type {
        INDIVIDUAL,
        COMPANY,
        BUDGET_COMPANY
    }

    @NotBlank
    private String name;

    @NotNull
    private Type type;

    @Valid
    private List<@ValidPhone String> phoneNumbers = new ArrayList<>();

    @NotEmpty(message = "Должен быть указан хотя бы 1 email адрес")
    @Valid
    private List<@ValidEmail String> emailAddresses = new ArrayList<>();

    @Valid
    private Address postalAddress;

    @Valid
    private Passport passport;

    @Valid
    private LegalEntity legalEntity;

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

    public List<String> getPhoneNumbers() {
        return phoneNumbers;
    }

    public void setPhoneNumbers(List<String> phoneNumbers) {
        this.phoneNumbers = phoneNumbers;
    }

    public List<String> getEmailAddresses() {
        return emailAddresses;
    }

    public void setEmailAddresses(List<String> emailAddresses) {
        this.emailAddresses = emailAddresses;
    }

    public Address getPostalAddress() {
        return postalAddress;
    }

    public void setPostalAddress(Address postalAddress) {
        this.postalAddress = postalAddress;
    }

    public Passport getPassport() {
        return passport;
    }

    public void setPassport(Passport passport) {
        this.passport = passport;
    }

    public LegalEntity getLegalEntity() {
        return legalEntity;
    }

    public void setLegalEntity(LegalEntity legalEntity) {
        this.legalEntity = legalEntity;
    }

    @Override
    public String toString() {
        return "AccountOwner{" +
                "name='" + name + '\'' +
                ", type=" + type +
                ", phoneNumbers=" + phoneNumbers +
                ", emailAddresses=" + emailAddresses +
                ", postalAddress=" + postalAddress +
                ", passport=" + passport +
                ", legalEntity=" + legalEntity +
                "} " + super.toString();
    }

    public static AccountOwner fromPerson(Person person) {
        AccountOwner accountOwner = new AccountOwner();
        accountOwner.setName(person.getName());
        accountOwner.setEmailAddresses(person.getEmailAddresses());
        accountOwner.setPhoneNumbers(person.getPhoneNumbers());

        Address address = Address.fromString(person.getPostalAddressAsString());
        address.setCountry(person.getCountry());

        accountOwner.setPostalAddress(address);

        accountOwner.setPersonalAccountId(person.getAccountId());
        accountOwner.setLegalEntity(LegalEntity.fromRcLegalEntity(person.getLegalEntity()));
        accountOwner.setPassport(Passport.fromRcPassport(person.getPassport()));

        if (person.getPassport() != null && person.getLegalEntity() != null) {
            accountOwner.setType(Type.COMPANY);
        } else if (person.getPassport() != null && person.getLegalEntity() == null) {
            accountOwner.setType(Type.INDIVIDUAL);
        }

        return accountOwner;
    }
}
