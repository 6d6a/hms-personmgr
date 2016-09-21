package ru.majordomo.hms.personmgr.common;

import java.util.HashMap;
import java.util.Map;

import static ru.majordomo.hms.personmgr.common.MailManagerMessageType.SMS_DOMAIN_DELEGATION_ENDING;
import static ru.majordomo.hms.personmgr.common.MailManagerMessageType.SMS_NEW_PAYMENT;
import static ru.majordomo.hms.personmgr.common.MailManagerMessageType.SMS_NO_MONEY_TO_AUTORENEW_DOMAIN;
import static ru.majordomo.hms.personmgr.common.MailManagerMessageType.SMS_REMAINING_DAYS;

/**
 * ImportConstants
 */
public class ImportConstants {

    private static final Map<Integer, MailManagerMessageType> smsNotifications = new HashMap<>();

    static {
        smsNotifications.put(24, SMS_NO_MONEY_TO_AUTORENEW_DOMAIN);
        smsNotifications.put(26, SMS_NEW_PAYMENT);
        smsNotifications.put(28, SMS_DOMAIN_DELEGATION_ENDING);
        smsNotifications.put(29, SMS_REMAINING_DAYS);
    }

    public static Map<Integer, MailManagerMessageType> getSmsNotifications() {
        return smsNotifications;
    }
}
