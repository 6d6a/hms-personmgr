package ru.majordomo.hms.personmgr.model.account;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Address {
    private String country;
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

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
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
                "country='" + country + '\'' +
                ", zip='" + zip + '\'' +
                ", street='" + street + '\'' +
                ", city='" + city + '\'' +
                '}';
    }

    public static Address fromString(String address) {
        return new Address(address);
    }

    public Address(String address) {
        if (address != null && !address.isEmpty()) {

            this.zip = findPostalIndexInAddressString(address);
            if (this.zip != null) {
                address = address.replaceAll(this.zip + "\\s?,?\\s?", "");
            }

            String[] addressParts = address.split(",");
            if (addressParts.length < 2) {
                addressParts = address.split(" ");
            }

            if (addressParts.length >= 2) {
                StringBuilder streetBuilder = new StringBuilder();

                this.city = addressParts[0].trim();

                for (int i = 1; i < addressParts.length; i++) {
                    streetBuilder.append(addressParts[i].trim());
                    streetBuilder.append(", ");
                }

                this.street = streetBuilder.toString().trim();
                if (!this.street.isEmpty() && this.street.charAt(this.street.length() - 1) == ',') {
                    this.street = this.street.substring(0, this.street.length() - 1);
                }
            } else {
                this.street = address;
            }
        }
    }

    public static String findPostalIndexInAddressString(String address) {
        String postalIndexPattern = "\\d{4,}";
        Pattern pattern = Pattern.compile(postalIndexPattern);
        Matcher matcher = pattern.matcher(address);

        return matcher.find() ? matcher.group() : null;
    }
}
