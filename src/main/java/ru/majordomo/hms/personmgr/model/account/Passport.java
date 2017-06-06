package ru.majordomo.hms.personmgr.model.account;

import java.time.LocalDate;

public class Passport {
    private String number;
    private String issuedOrg;
    private LocalDate issuedDate;

    private String address;

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

    @Override
    public String toString() {
        return "Passport{" +
                "number='" + number + '\'' +
                ", issuedOrg='" + issuedOrg + '\'' +
                ", issuedDate=" + issuedDate +
                ", address=" + address +
                '}';
    }
}
