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

    private static final String PartnerPromocodeActionId = "57f3c4b8038d8a6054409853";
    private static final String BonusFreeDomainPromocodeActionId = "57f3c4b8038d8a6054409854";
    private static final String BonusUnlimited3MPromocodeActionId = "57f3c4b8038d8a6054409855";
    private static final String BonusUnlimited1MPromocodeActionId = "57f3c4b8038d8a6054409856";
    private static final String BonusParking3MPromocodeActionId = "57f3c4b8038d8a6054409857";

    //TODO указать конкретный id
    private static final String UnlimitedPlanServiceId = "1";
    private static final String ParkingPlanServiceId = "2";

    private static final Map<Integer, MailManagerMessageType> MANAGER_MESSAGE_TYPE_MAP = new HashMap<>();
    public static final Map<String, DomainCategory> DOMAIN_CATEGORY_MAP = new HashMap<>();
    public static final Map<Integer, DomainRegistrator> DOMAIN_REGISTRATOR_MAP = new HashMap<>();
    public static final Map<String, DomainRegistrator> DOMAIN_REGISTRATOR_STRING_MAP = new HashMap<>();
    public static final Map<Integer, String> DOMAIN_REGISTRATOR_NAME_MAP = new HashMap<>();

    static {
        MANAGER_MESSAGE_TYPE_MAP.put(24, SMS_NO_MONEY_TO_AUTORENEW_DOMAIN);
        MANAGER_MESSAGE_TYPE_MAP.put(26, SMS_NEW_PAYMENT);
        MANAGER_MESSAGE_TYPE_MAP.put(28, SMS_DOMAIN_DELEGATION_ENDING);
        MANAGER_MESSAGE_TYPE_MAP.put(29, SMS_REMAINING_DAYS);
        MANAGER_MESSAGE_TYPE_MAP.put(42, EMAIL_CHANGE_ACCOUNT_PASSWORD);
        MANAGER_MESSAGE_TYPE_MAP.put(44, EMAIL_LOGIN_TO_CONTROL_PANEL);
        MANAGER_MESSAGE_TYPE_MAP.put(77, EMAIL_CHANGE_FTP_PASSWORD);

        DOMAIN_CATEGORY_MAP.put("russian", DomainCategory.RUSSIAN);
        DOMAIN_CATEGORY_MAP.put("cyrillic", DomainCategory.CYRILLIC);
        DOMAIN_CATEGORY_MAP.put("international", DomainCategory.INTERNATIONAL);
        DOMAIN_CATEGORY_MAP.put("business", DomainCategory.BUSINESS);
        DOMAIN_CATEGORY_MAP.put("thematic", DomainCategory.THEMATIC);
        DOMAIN_CATEGORY_MAP.put("geo", DomainCategory.GEO);

        DOMAIN_REGISTRATOR_MAP.put(1, DomainRegistrator.REGISTRANT);
        DOMAIN_REGISTRATOR_MAP.put(2, DomainRegistrator.R01);
        DOMAIN_REGISTRATOR_MAP.put(3, DomainRegistrator.RUCENTER);
        DOMAIN_REGISTRATOR_MAP.put(4, DomainRegistrator.ENOM);
        DOMAIN_REGISTRATOR_MAP.put(5, DomainRegistrator.GODADDY);
        DOMAIN_REGISTRATOR_MAP.put(6, DomainRegistrator.GANDI);
        DOMAIN_REGISTRATOR_MAP.put(7, DomainRegistrator.UKRNAMES);
        DOMAIN_REGISTRATOR_MAP.put(8, DomainRegistrator.REGRU);
        DOMAIN_REGISTRATOR_MAP.put(9, DomainRegistrator.WEBNAMES);

        DOMAIN_REGISTRATOR_STRING_MAP.put("Registrant", DomainRegistrator.REGISTRANT);
        DOMAIN_REGISTRATOR_STRING_MAP.put("GPT", DomainRegistrator.R01);
        DOMAIN_REGISTRATOR_STRING_MAP.put("RUCENTER", DomainRegistrator.RUCENTER);
        DOMAIN_REGISTRATOR_STRING_MAP.put("Enom", DomainRegistrator.ENOM);
        DOMAIN_REGISTRATOR_STRING_MAP.put("GoDaddy", DomainRegistrator.GODADDY);
        DOMAIN_REGISTRATOR_STRING_MAP.put("Ukrnames", DomainRegistrator.UKRNAMES);
        DOMAIN_REGISTRATOR_STRING_MAP.put("RegRu", DomainRegistrator.REGRU);
        DOMAIN_REGISTRATOR_STRING_MAP.put("Webnames", DomainRegistrator.WEBNAMES);

        DOMAIN_REGISTRATOR_NAME_MAP.put(1, "Регистрант");
        DOMAIN_REGISTRATOR_NAME_MAP.put(2, "R01");
        DOMAIN_REGISTRATOR_NAME_MAP.put(3, "RU-CENTER");
        DOMAIN_REGISTRATOR_NAME_MAP.put(4, "Enom");
        DOMAIN_REGISTRATOR_NAME_MAP.put(5, "GoDaddy");
        DOMAIN_REGISTRATOR_NAME_MAP.put(6, "Gandi");
        DOMAIN_REGISTRATOR_NAME_MAP.put(7, "Ukrnames");
        DOMAIN_REGISTRATOR_NAME_MAP.put(8, "РЕГ.РУ");
        DOMAIN_REGISTRATOR_NAME_MAP.put(9, "Webnames");
    }


    public static Map<Integer, MailManagerMessageType> getManagerMessageTypeMap() {
        return MANAGER_MESSAGE_TYPE_MAP;
    }

    public static String getPartnerPromocodeActionId() {
        return PartnerPromocodeActionId;
    }

    public static String getBonusFreeDomainPromocodeActionId() {
        return BonusFreeDomainPromocodeActionId;
    }

    public static String getBonusUnlimited3MPromocodeActionId() {
        return BonusUnlimited3MPromocodeActionId;
    }

    public static String getBonusUnlimited1MPromocodeActionId() {
        return BonusUnlimited1MPromocodeActionId;
    }

    public static String getBonusParking3MPromocodeActionId() {
        return BonusParking3MPromocodeActionId;
    }

    public static String getUnlimitedPlanServiceId() {
        return UnlimitedPlanServiceId;
    }

    public static String getParkingPlanServiceId() {
        return ParkingPlanServiceId;
    }
}
