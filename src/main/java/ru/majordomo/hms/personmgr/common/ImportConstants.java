package ru.majordomo.hms.personmgr.common;

import java.util.HashMap;
import java.util.Map;

import static ru.majordomo.hms.personmgr.common.MailManagerMessageType.EMAIL_CHANGE_ACCOUNT_PASSWORD;
import static ru.majordomo.hms.personmgr.common.MailManagerMessageType.EMAIL_CHANGE_FTP_PASSWORD;
import static ru.majordomo.hms.personmgr.common.MailManagerMessageType.EMAIL_LOGIN_TO_CONTROL_PANEL;
import static ru.majordomo.hms.personmgr.common.MailManagerMessageType.SMS_DOMAIN_DELEGATION_ENDING;
import static ru.majordomo.hms.personmgr.common.MailManagerMessageType.SMS_NEW_PAYMENT;
import static ru.majordomo.hms.personmgr.common.MailManagerMessageType.SMS_NO_MONEY_TO_AUTORENEW_DOMAIN;
import static ru.majordomo.hms.personmgr.common.MailManagerMessageType.SMS_REMAINING_DAYS;

/**
 * ImportConstants
 */
public class ImportConstants {

    private static final Map<Integer, MailManagerMessageType> notifications = new HashMap<>();

    static {
        notifications.put(24, SMS_NO_MONEY_TO_AUTORENEW_DOMAIN);
        notifications.put(26, SMS_NEW_PAYMENT);
        notifications.put(28, SMS_DOMAIN_DELEGATION_ENDING);
        notifications.put(29, SMS_REMAINING_DAYS);
        notifications.put(42, EMAIL_CHANGE_ACCOUNT_PASSWORD);
        notifications.put(44, EMAIL_LOGIN_TO_CONTROL_PANEL);
        notifications.put(77, EMAIL_CHANGE_FTP_PASSWORD);
    }

    public static Map<Integer, MailManagerMessageType> getNotifications() {
        return notifications;
    }
}
