package ru.majordomo.hms.personmgr.model.account;

import java.time.LocalDate;

import javax.validation.Valid;

public class Passport {
    private String number;
    private String issuedOrg;
    private LocalDate issuedDate;

    @Valid
    private Address address;

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

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
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

    public static Passport fromRcPassport(ru.majordomo.hms.rc.user.resources.Passport rcPassport) {
        if (rcPassport == null) {
            return null;
        }

        Passport passport = new Passport();
        passport.setNumber(rcPassport.getNumber());
        passport.setIssuedDate(rcPassport.getIssuedDate());
        passport.setIssuedOrg(rcPassport.getIssuedOrg());
        passport.setAddress(Address.fromString(rcPassport.getAddress()));

        return passport;
    }
}
