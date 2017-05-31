package ru.majordomo.hms.personmgr.model;

public class Address {
    private String zip;
    private String street;
    private String city;

    public Address() {
    }

    public Address(String zip, String street, String city) {
        this.zip = zip;
        this.street = street;
        this.city = city;
    }

    public String getZip() {
        return zip;
    }

    public void setZip(String zip) {
        this.zip = zip;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    @Override
    public String toString() {
        return "Address{" +
                "zip=" + zip +
                ", street='" + street + '\'' +
                ", city='" + city + '\'' +
                '}';
    }
}
