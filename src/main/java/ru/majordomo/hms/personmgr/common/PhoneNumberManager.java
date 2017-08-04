package ru.majordomo.hms.personmgr.common;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

public class PhoneNumberManager {
    private static PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();

    public static Phonenumber.PhoneNumber parsePhone(String phone) {
        try {
            return phoneNumberUtil.parse(phone, "RU");
        } catch (NumberParseException e) {
            return null;
        }
    }

    public static String formatPhone(String phone) {
        Phonenumber.PhoneNumber phoneNumber = parsePhone(phone);

        if (phoneNumber == null) {
            return "";
        }

        return phoneNumberUtil.format(phoneNumber, PhoneNumberUtil.PhoneNumberFormat.E164);
    }

    public static Boolean phoneValid(String phone) {
        Phonenumber.PhoneNumber phoneNumber = parsePhone(phone);

        return phoneNumber != null && phoneNumberUtil.isValidNumber(phoneNumber);
    }
}
