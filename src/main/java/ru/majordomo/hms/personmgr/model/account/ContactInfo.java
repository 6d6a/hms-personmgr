package ru.majordomo.hms.personmgr.model.account;


import org.hibernate.validator.constraints.NotEmpty;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

import javax.validation.Valid;
import javax.validation.constraints.Null;

import ru.majordomo.hms.personmgr.common.Utils;
import ru.majordomo.hms.personmgr.validation.ValidPhone;
import ru.majordomo.hms.personmgr.validation.group.AccountOwnerIndividualChecks;
import ru.majordomo.hms.rc.user.resources.validation.ValidEmail;

public class ContactInfo {
    @Valid
    private List<@ValidPhone String> phoneNumbers = new ArrayList<>();

    @NotEmpty(message = "Должен быть указан хотя бы 1 email адрес")
    @Valid
    private List<@ValidEmail String> emailAddresses = new ArrayList<>();

    private String postalAddress;

    @Null(groups = {AccountOwnerIndividualChecks.class}, message = "Физическое лицо не может заполнять поле 'Банк'")
    private String bankName;

    @Null(groups = {AccountOwnerIndividualChecks.class}, message = "Физическое лицо не может заполнять поле 'Бик'")
    private String bik;

    @Null(groups = {AccountOwnerIndividualChecks.class}, message = "Физическое лицо не может заполнять поле 'Кор. счет'")
    private String correspondentAccount;

    @Null(groups = {AccountOwnerIndividualChecks.class}, message = "Физическое лицо не может заполнять поле 'Счет'")
    private String bankAccount;

    public List<String> getPhoneNumbers() {
        return phoneNumbers;
    }

    public void setPhoneNumbers(List<String> phoneNumbers) {
        this.phoneNumbers = phoneNumbers;
    }

    public void addPhoneNumber(String phoneNumber) {
        if (phoneNumbers == null) {
            phoneNumbers = new ArrayList<>();
        }

        phoneNumbers.add(phoneNumber);
    }

    public List<String> getEmailAddresses() {
        return emailAddresses;
    }

    public void setEmailAddresses(List<String> emailAddresses) {
        this.emailAddresses = emailAddresses;
    }

    public void addEmailAddress(String emailAddress) {
        if (emailAddresses == null) {
            emailAddresses = new ArrayList<>();
        }

        emailAddresses.add(emailAddress);
    }

    public String getPostalAddress() {
        return postalAddress;
    }

    public void setPostalAddress(String postalAddress) {
        this.postalAddress = postalAddress;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public String getBik() {
        return bik;
    }

    public void setBik(String bik) {
        this.bik = bik;
    }

    public String getCorrespondentAccount() {
        return correspondentAccount;
    }

    public void setCorrespondentAccount(String correspondentAccount) {
        this.correspondentAccount = correspondentAccount;
    }

    public String getBankAccount() {
        return bankAccount;
    }

    public void setBankAccount(String bankAccount) {
        this.bankAccount = bankAccount;
    }

    public ContactInfo() {}

    public ContactInfo(ContactInfo contactInfo) {
        super();
        this.setEmailAddresses(new ArrayList<>(contactInfo.getEmailAddresses()));
        this.setPhoneNumbers(new ArrayList<>(contactInfo.getPhoneNumbers()));
        this.setPostalAddress(contactInfo.getPostalAddress());
        this.setBankAccount(contactInfo.getBankAccount());
        this.setBankName(contactInfo.getBankName());
        this.setBik(contactInfo.getBik());
        this.setCorrespondentAccount(contactInfo.getCorrespondentAccount());
    }

    public String getDiffMessage(ContactInfo contactInfo){
        StringJoiner joiner = new StringJoiner(", ");
        joiner.add(Utils.diffFieldsString("номера телефонов", getPhoneNumbers(), contactInfo.getPhoneNumbers()));
        joiner.add(Utils.diffFieldsString("email-адреса", getEmailAddresses(), contactInfo.getEmailAddresses()));
        joiner.add(Utils.diffFieldsString("почтовый адрес", getPostalAddress(), contactInfo.getPostalAddress()));
        joiner.add(Utils.diffFieldsString("банк", getBankName(), contactInfo.getBankName()));
        joiner.add(Utils.diffFieldsString("Бик", getBik(), contactInfo.getBik()));
        joiner.add(Utils.diffFieldsString("Кор. счет", getCorrespondentAccount(), contactInfo.getCorrespondentAccount()));
        joiner.add(Utils.diffFieldsString("счет", getBankAccount(), contactInfo.getBankAccount()));
        return joiner.toString();
    }

    @Override
    public String toString() {
        return "ContactInfo{" +
                "phoneNumbers=" + phoneNumbers +
                ", emailAddresses=" + emailAddresses +
                ", postalAddress='" + postalAddress + '\'' +
                ", bankName='" + bankName + '\'' +
                ", bik='" + bik + '\'' +
                ", correspondentAccount='" + correspondentAccount + '\'' +
                ", bankAccount='" + bankAccount + '\'' +
                '}';
    }
}
