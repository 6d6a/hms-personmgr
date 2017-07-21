package ru.majordomo.hms.personmgr.common;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

public class PhoneNumberManager {
    private static PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();

    public static Boolean phoneValid(String phone) {
        Phonenumber.PhoneNumber phoneNumber;
        try {
            phoneNumber = phoneNumberUtil.parse(phone, "RU");
            return phoneNumberUtil.isValidNumber(phoneNumber);
        } catch (NumberParseException e) {
            return false;
        }
    }

}
