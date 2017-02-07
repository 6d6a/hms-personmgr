package ru.majordomo.hms.personmgr.common;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import ru.majordomo.hms.rc.user.resources.DomainRegistrar;

import static ru.majordomo.hms.personmgr.common.MailManagerMessageType.EMAIL_CHANGE_ACCOUNT_PASSWORD;
import static ru.majordomo.hms.personmgr.common.MailManagerMessageType.EMAIL_CHANGE_FTP_PASSWORD;
import static ru.majordomo.hms.personmgr.common.MailManagerMessageType.EMAIL_LOGIN_TO_CONTROL_PANEL;
import static ru.majordomo.hms.personmgr.common.MailManagerMessageType.SMS_DOMAIN_DELEGATION_ENDING;
import static ru.majordomo.hms.personmgr.common.MailManagerMessageType.SMS_NEW_PAYMENT;
import static ru.majordomo.hms.personmgr.common.MailManagerMessageType.SMS_NO_MONEY_TO_AUTORENEW_DOMAIN;
import static ru.majordomo.hms.personmgr.common.MailManagerMessageType.SMS_REMAINING_DAYS;

public class Constants {
    public static final String TECHNICAL_ACCOUNT_ID = "999";
    public static final String VH_ACCOUNT_PREFIX = "AC_";
    public static final String PLAN_SERVICE_PREFIX = "plan_";
    public static final String PLAN_SERVICE_ABONEMENT_PREFIX = PLAN_SERVICE_PREFIX + "abonement_";
    public static final String SEO_SERVICE_PREFIX = "seo_";
    public static final String SEO_AUDIT_SERVICE_PREFIX = SEO_SERVICE_PREFIX + "audit_";
    public static final String SEO_CONTEXT_SERVICE_PREFIX = SEO_SERVICE_PREFIX + "context_";
    public static final String SEO_AUDIT_SERVICE_ID = "1";
    public static final String SEO_CONTEXT_SERVICE_ID = "2";
    public static final String BONUS_PAYMENT_TYPE_ID = "31";
    public static final String FREE_SERVICE_POSTFIX = "_free";
    public static final String FREE_SERVICE_NAME_POSTFIX = " (бесплатно)";
    public static final String SERVICE_PREFIX = "service_";
    public static final String REGISTRATION_COST_SERVICE_PREFIX = "registration_cost_";
    public static final String RENEW_COST_SERVICE_PREFIX = "renew_cost_";
    public static final String SERVICE_MONEY_RETURN_PREFIX = "service_money_return_";
    public static final String SERVICE_MONEY_TRANSFER_PREFIX = "service_money_transfer_";
    public static final String ACCOUNT_SETTING_OVERQUOTED = "overquoted";
    public static final String ACCOUNT_SETTING_ADD_QUOTA_IF_OVERQUOTED = "add_quota_if_overquoted";
    public static final String ACCOUNT_SETTING_AUTO_BILL_SENDING = "auto_bill_sending";
    public static final String ACCOUNT_SETTING_CREDIT = "credit";
    public static final String ACCOUNT_SETTING_NOTIFY_DAYS = "notify_days";
    public static final int PLAN_PARKING_ID = 109;
    public static final int PLAN_PARKING_PLUS_ID = 124;
    public static final int PLAN_UNLIMITED_ID = 9802;
    public static final String PLAN_UNLIMITED_SERVICE_ID = PLAN_SERVICE_PREFIX + PLAN_UNLIMITED_ID;
    public static final int PLAN_PARKING_DOMAINS_ID = 9803;
    public static final String PLAN_PARKING_DOMAINS_SERVICE_ID = PLAN_SERVICE_PREFIX + PLAN_PARKING_DOMAINS_ID;
    public static final int PLAN_START_ID = 9804;
    public static final int PLAN_UNLIMITED_PLUS_ID = 9805;
    public static final int PLAN_BUSINESS_ID = 9806;
    public static final int PLAN_BUSINESS_PLUS_ID = 9807;

    public static final int ADDITIONAL_WEB_SITE_ID = 22;
    public static final String ADDITIONAL_WEB_SITE_SERVICE_ID = SERVICE_PREFIX + ADDITIONAL_WEB_SITE_ID;

    public static final int ADDITIONAL_FTP_ID = 4;
    public static final String ADDITIONAL_FTP_SERVICE_ID = SERVICE_PREFIX + ADDITIONAL_FTP_ID;
    public static final int ADDITIONAL_FTP_FREE_ID = 20;
    public static final String ADDITIONAL_FTP_FREE_SERVICE_ID = SERVICE_PREFIX + ADDITIONAL_FTP_FREE_ID;

    public static final int SMS_NOTIFICATIONS_10_RUB_ID = 18;
    public static final String SMS_NOTIFICATIONS_10_RUB_SERVICE_ID = SERVICE_PREFIX + SMS_NOTIFICATIONS_10_RUB_ID;
    public static final String SMS_NOTIFICATIONS_FREE_SERVICE_ID = SERVICE_PREFIX + SMS_NOTIFICATIONS_10_RUB_ID + FREE_SERVICE_POSTFIX;
    public static final int SMS_NOTIFICATIONS_29_RUB_ID = 21;
    public static final String SMS_NOTIFICATIONS_29_RUB_SERVICE_ID = SERVICE_PREFIX + SMS_NOTIFICATIONS_29_RUB_ID;

    public static final int ADDITIONAL_QUOTA_100_ID = 15;
    public static final String ADDITIONAL_QUOTA_100_SERVICE_ID = SERVICE_PREFIX + ADDITIONAL_QUOTA_100_ID;
    public static final long ADDITIONAL_QUOTA_100_CAPACITY = 102400L;

    public static final int PLAN_PROPERTY_LIMIT_UNLIMITED = -1;

    public static final Map<String, DomainCategory> DOMAIN_CATEGORY_MAP = new HashMap<>();
    public static final Map<Integer, DomainRegistrar> DOMAIN_REGISTRAR_MAP = new HashMap<>();
    public static final Map<String, DomainRegistrar> DOMAIN_REGISTRAR_STRING_MAP = new HashMap<String, DomainRegistrar>();
    public static final Map<Integer, String> DOMAIN_REGISTRATOR_NAME_MAP = new HashMap<>();

    public static final Set<Integer> NOT_NEEDED_SERVICE_IDS = new HashSet<>();
    public static final Set<Integer> OPTIONALLY_FREE_SERVICE_IDS = new HashSet<>();

    public static final String PARTNER_PROMOCODE_ACTION_ID = "57f3c4b8038d8a6054409853";
    public static final String BONUS_FREE_DOMAIN_PROMOCODE_ACTION_ID = "57f3c4b8038d8a6054409854";
    public static final String BONUS_UNLIMITED_3_M_PROMOCODE_ACTION_ID = "57f3c4b8038d8a6054409855";
    public static final String BONUS_UNLIMITED_1_M_PROMOCODE_ACTION_ID = "57f3c4b8038d8a6054409856";
    public static final String BONUS_PARKING_3_M_PROMOCODE_ACTION_ID = "57f3c4b8038d8a6054409857";

    public static final Map<Integer, MailManagerMessageType> MANAGER_MESSAGE_TYPE_MAP = new HashMap<>();

    static {
        Constants.MANAGER_MESSAGE_TYPE_MAP.put(24, SMS_NO_MONEY_TO_AUTORENEW_DOMAIN);
        Constants.MANAGER_MESSAGE_TYPE_MAP.put(26, SMS_NEW_PAYMENT);
        Constants.MANAGER_MESSAGE_TYPE_MAP.put(28, SMS_DOMAIN_DELEGATION_ENDING);
        Constants.MANAGER_MESSAGE_TYPE_MAP.put(29, SMS_REMAINING_DAYS);
        Constants.MANAGER_MESSAGE_TYPE_MAP.put(42, EMAIL_CHANGE_ACCOUNT_PASSWORD);
        Constants.MANAGER_MESSAGE_TYPE_MAP.put(44, EMAIL_LOGIN_TO_CONTROL_PANEL);
        Constants.MANAGER_MESSAGE_TYPE_MAP.put(77, EMAIL_CHANGE_FTP_PASSWORD);

        Constants.DOMAIN_CATEGORY_MAP.put("russian", DomainCategory.RUSSIAN);
        Constants.DOMAIN_CATEGORY_MAP.put("cyrillic", DomainCategory.CYRILLIC);
        Constants.DOMAIN_CATEGORY_MAP.put("international", DomainCategory.INTERNATIONAL);
        Constants.DOMAIN_CATEGORY_MAP.put("business", DomainCategory.BUSINESS);
        Constants.DOMAIN_CATEGORY_MAP.put("thematic", DomainCategory.THEMATIC);
        Constants.DOMAIN_CATEGORY_MAP.put("geo", DomainCategory.GEO);

        Constants.DOMAIN_REGISTRAR_MAP.put(1, DomainRegistrar.NETHOUSE);
        Constants.DOMAIN_REGISTRAR_MAP.put(2, DomainRegistrar.R01);
        Constants.DOMAIN_REGISTRAR_MAP.put(3, DomainRegistrar.RUCENTER);
        Constants.DOMAIN_REGISTRAR_MAP.put(4, DomainRegistrar.ENOM);
        Constants.DOMAIN_REGISTRAR_MAP.put(5, DomainRegistrar.GODADDY);
        Constants.DOMAIN_REGISTRAR_MAP.put(6, DomainRegistrar.GANDI);
        Constants.DOMAIN_REGISTRAR_MAP.put(7, DomainRegistrar.UKRNAMES);
        Constants.DOMAIN_REGISTRAR_MAP.put(8, DomainRegistrar.REGRU);
        Constants.DOMAIN_REGISTRAR_MAP.put(9, DomainRegistrar.WEBNAMES);

        Constants.DOMAIN_REGISTRAR_STRING_MAP.put("Registrant", DomainRegistrar.NETHOUSE);
        Constants.DOMAIN_REGISTRAR_STRING_MAP.put("GPT", DomainRegistrar.R01);
        Constants.DOMAIN_REGISTRAR_STRING_MAP.put("RUCENTER", DomainRegistrar.RUCENTER);
        Constants.DOMAIN_REGISTRAR_STRING_MAP.put("Enom", DomainRegistrar.ENOM);
        Constants.DOMAIN_REGISTRAR_STRING_MAP.put("GoDaddy", DomainRegistrar.GODADDY);
        Constants.DOMAIN_REGISTRAR_STRING_MAP.put("Ukrnames", DomainRegistrar.UKRNAMES);
        Constants.DOMAIN_REGISTRAR_STRING_MAP.put("RegRu", DomainRegistrar.REGRU);
        Constants.DOMAIN_REGISTRAR_STRING_MAP.put("Webnames", DomainRegistrar.WEBNAMES);

        Constants.DOMAIN_REGISTRATOR_NAME_MAP.put(1, "Nethouse");
        Constants.DOMAIN_REGISTRATOR_NAME_MAP.put(2, "R01");
        Constants.DOMAIN_REGISTRATOR_NAME_MAP.put(3, "RU-CENTER");
        Constants.DOMAIN_REGISTRATOR_NAME_MAP.put(4, "Enom");
        Constants.DOMAIN_REGISTRATOR_NAME_MAP.put(5, "GoDaddy");
        Constants.DOMAIN_REGISTRATOR_NAME_MAP.put(6, "Gandi");
        Constants.DOMAIN_REGISTRATOR_NAME_MAP.put(7, "Ukrnames");
        Constants.DOMAIN_REGISTRATOR_NAME_MAP.put(8, "РЕГ.РУ");
        Constants.DOMAIN_REGISTRATOR_NAME_MAP.put(9, "Webnames");

//        Constants.NOT_NEEDED_SERVICE_IDS.add(1);
        Constants.NOT_NEEDED_SERVICE_IDS.add(2);
        Constants.NOT_NEEDED_SERVICE_IDS.add(3);
        Constants.NOT_NEEDED_SERVICE_IDS.add(5);
        Constants.NOT_NEEDED_SERVICE_IDS.add(7);
        Constants.NOT_NEEDED_SERVICE_IDS.add(8);
        Constants.NOT_NEEDED_SERVICE_IDS.add(10);
        Constants.NOT_NEEDED_SERVICE_IDS.add(11);
        Constants.NOT_NEEDED_SERVICE_IDS.add(14);
        Constants.NOT_NEEDED_SERVICE_IDS.add(16);
        Constants.NOT_NEEDED_SERVICE_IDS.add(17);
        Constants.NOT_NEEDED_SERVICE_IDS.add(19);
        Constants.NOT_NEEDED_SERVICE_IDS.add(20);
        Constants.NOT_NEEDED_SERVICE_IDS.add(21);

        //ДопFTP
        Constants.OPTIONALLY_FREE_SERVICE_IDS.add(4);
        //Защита от спама
        Constants.OPTIONALLY_FREE_SERVICE_IDS.add(13);
        //СМС-уведомления
        Constants.OPTIONALLY_FREE_SERVICE_IDS.add(18);
    }
}