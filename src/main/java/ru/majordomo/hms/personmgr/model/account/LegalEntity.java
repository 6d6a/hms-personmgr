package ru.majordomo.hms.personmgr.model.account;

import javax.validation.Valid;

public class LegalEntity {
    private String inn;
    private String okpo;
    private String kpp;
    private String ogrn;
    private String okvedCodes;

    @Valid
    private Address address;

    private String bankName;
    private String bik;
    private String correspondentAccount;
    private String bankAccount;

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
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

    @Override
    public String toString() {
        return "LegalEntity{" +
                "inn='" + inn + '\'' +
                ", okpo='" + okpo + '\'' +
                ", kpp='" + kpp + '\'' +
                ", ogrn='" + ogrn + '\'' +
                ", okvedCodes='" + okvedCodes + '\'' +
                ", address='" + address + '\'' +
                '}';
    }

    public static LegalEntity fromRcLegalEntity(ru.majordomo.hms.rc.user.resources.LegalEntity rcLegalEntity) {
        if (rcLegalEntity == null) {
            return null;
        }

        LegalEntity legalEntity = new LegalEntity();
        legalEntity.setInn(rcLegalEntity.getInn());
        legalEntity.setOgrn(rcLegalEntity.getOgrn());
        legalEntity.setKpp(rcLegalEntity.getKpp());
        legalEntity.setOkpo(rcLegalEntity.getOkpo());
        legalEntity.setOkvedCodes(rcLegalEntity.getOkvedCodes());
        legalEntity.setBankAccount(rcLegalEntity.getBankAccount());
        legalEntity.setBankName(rcLegalEntity.getBankName());
        legalEntity.setCorrespondentAccount(rcLegalEntity.getCorrespondentAccount());
        legalEntity.setBik(rcLegalEntity.getBik());
        legalEntity.setAddress(Address.fromString(rcLegalEntity.getAddress()));

        return legalEntity;
    }
}
