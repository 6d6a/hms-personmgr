package ru.majordomo.hms.personmgr.model.account;


import java.time.LocalDate;

import javax.validation.constraints.Null;

import ru.majordomo.hms.personmgr.common.Utils;
import ru.majordomo.hms.personmgr.validation.group.AccountOwnerCompanyChecks;
import ru.majordomo.hms.personmgr.validation.group.AccountOwnerIndividualChecks;

public class PersonalInfo {
    @Null(groups = {AccountOwnerCompanyChecks.class}, message = "Юридическое лицо не может заполнять поле 'номер паспорта'")
    private String number;

    @Null(groups = {AccountOwnerCompanyChecks.class}, message = "Юридическое лицо не может заполнять поле 'паспорт выдан'")
    private String issuedOrg;

    @Null(groups = {AccountOwnerCompanyChecks.class}, message = "Юридическое лицо не может заполнять поле 'дата выдачи паспорта'")
    private LocalDate issuedDate;

    private String address;

    @Null(groups = {AccountOwnerIndividualChecks.class}, message = "Физическое лицо не может заполнять поле 'ИНН'")
    private String inn;

    @Null(groups = {AccountOwnerIndividualChecks.class}, message = "Физическое лицо не может заполнять поле 'ОКПО'")
    private String okpo;

    @Null(groups = {AccountOwnerIndividualChecks.class}, message = "Физическое лицо не может заполнять поле 'КПП'")
    private String kpp;

    @Null(groups = {AccountOwnerIndividualChecks.class}, message = "Физическое лицо не может заполнять поле 'ОГРН'")
    private String ogrn;

    @Null(groups = {AccountOwnerIndividualChecks.class}, message = "Физическое лицо не может заполнять поле 'ОКВЭД'")
    private String okvedCodes;

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getIssuedOrg() {
        return issuedOrg;
    }

    public void setIssuedOrg(String issuedOrg) {
        this.issuedOrg = issuedOrg;
    }

    public LocalDate getIssuedDate() {
        return issuedDate;
    }

    public void setIssuedDate(LocalDate issuedDate) {
        this.issuedDate = issuedDate;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getInn() {
        return inn;
    }

    public void setInn(String inn) {
        this.inn = inn;
    }

    public String getOkpo() {
        return okpo;
    }

    public void setOkpo(String okpo) {
        this.okpo = okpo;
    }

    public String getKpp() {
        return kpp;
    }

    public void setKpp(String kpp) {
        this.kpp = kpp;
    }

    public String getOgrn() {
        return ogrn;
    }

    public void setOgrn(String ogrn) {
        this.ogrn = ogrn;
    }

    public String getOkvedCodes() {
        return okvedCodes;
    }

    public void setOkvedCodes(String okvedCodes) {
        this.okvedCodes = okvedCodes;
    }

    public String getDiffMessage(PersonalInfo personalInfo){
        return Utils.joinStringsWithDelimeterExceptNullStrings(
                ", ",
                Utils.diffFieldsString("номер паспорта", getNumber(), personalInfo.getNumber()),
                Utils.diffFieldsString("паспорт выдан", getIssuedOrg(), personalInfo.getIssuedOrg()),
                Utils.diffFieldsString("дата выдачи паспорта", getIssuedDate(), personalInfo.getIssuedDate()),
                Utils.diffFieldsString("юридический адрес", getAddress(), personalInfo.getAddress()),
                Utils.diffFieldsString("ИНН", getInn(), personalInfo.getInn()),
                Utils.diffFieldsString("ОКПО", getOkpo(), personalInfo.getOkpo()),
                Utils.diffFieldsString("КПП", getKpp(), personalInfo.getKpp()),
                Utils.diffFieldsString("ОГРН", getOgrn(), personalInfo.getOgrn()),
                Utils.diffFieldsString("ОКВЭД", getOkvedCodes(), personalInfo.getOkvedCodes())
        );
    }

    @Override
    public String toString() {
        return "PersonalInfo{" +
                "number='" + number + '\'' +
                ", issuedOrg='" + issuedOrg + '\'' +
                ", issuedDate=" + issuedDate +
                ", address='" + address + '\'' +
                ", inn='" + inn + '\'' +
                ", okpo='" + okpo + '\'' +
                ", kpp='" + kpp + '\'' +
                ", ogrn='" + ogrn + '\'' +
                ", okvedCodes='" + okvedCodes + '\'' +
                '}';
    }
}
