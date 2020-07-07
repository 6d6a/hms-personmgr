package ru.majordomo.hms.personmgr.common;

import java.math.BigDecimal;
import java.util.*;

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
    public static final String BONUS_PARTNER_TYPE_ID = "35";
    public static final String REAL_PAYMENT_TYPE_KIND = "REAL";
    public static final String CREDIT_PAYMENT_TYPE_KIND = "CREDIT";
    public static final String BONUS_PARTNER_PERCENT = "0.25";
    public static final String INCREASED_BONUS_PARTNER_PERCENT = "0.3";
    public static final String INCREASED_BONUS_PARTNER_DATE = "2018-01-25 00:00:00";
    @Deprecated
    public static final String ADDITIONAL_QUOTA_PLAN_ONLY_NAME = "unlimited-plus";
    public static final String FREE_SERVICE_POSTFIX = "_free";
    public static final String FREE_SERVICE_NAME_POSTFIX = " (бесплатно)";
    public static final String SERVICE_PREFIX = "service_";
    public static final String SERVICE_OLD_PREFIX = "service_old_";
    public static final String REGISTRATION_COST_SERVICE_PREFIX = "registration_cost_";
    public static final String RENEW_COST_SERVICE_PREFIX = "renew_cost_";
    public static final String SERVICE_MONEY_RETURN_PREFIX = "service_money_return_";
    public static final String SERVICE_MONEY_TRANSFER_PREFIX = "service_money_transfer_";
    public static final String RESOURCE_ID_KEY = "resourceId";
    public static final String TEMPLATE_ID_KEY = "templateId";
    public static final String ARCHIVED_RESOURCE_ID_KEY = "archivedResourceId";
    public static final String DOMAIN_NAME_KEY = "domainName";
    public static final String ACCOUNT_ID_KEY = "accountId";
    public static final String PERSON_ID_KEY = "personId";
    public static final String PASSWORD_KEY = "password";
    public static final String METADATA = "metadata";
    public static final String OAUTH_CLIENT_ID = "oauthClientId";
    public static final String MOBILE_APP_OAUTH_CLIENT_ID = "mobile_app";
    public static final String NAME_KEY = "name";
    public static final String OWNER_NAME_KEY = "ownerName";
    public static final String ACCOUNT_WAS_ACTIVE_KEY = "accountWasActive";
    public static final String TYPES_KEY = "types";
    public static final String TOKEN_KEY = "token";
    public static final String TYPE_KEY = "type";
    public static final String ACC_ID_KEY = "acc_id";
    public static final String DELETE_KEY = "delete";
    public static final String ERROR_MESSAGE_KEY = "errorMessage";
    public static final String IP_KEY = "ip";
    public static final String CLIENT_ID_KEY = "client_id";
    public static final String EMAIL_KEY = "email";
    public static final String PRIORITY_KEY = "priority";
    public static final String PARAMETRS_KEY = "parametrs";
    public static final String SEND_ONLY_TO_ACTIVE_KEY = "send_only_to_active";
    public static final String API_NAME_KEY = "api_name";
    public static final String SERVICE_NAME_KEY = "serviceName";
    public static final String DOMAINS_KEY = "domains";
    public static final String HISTORY_MESSAGE_KEY = "historyMessage";
    public static final String OPERATOR_KEY = "operator";
    public static final String AUTO_RENEW_KEY = "autoRenew";
    public static final String ENABLED_KEY = "enabled";
    public static final String AMOUNT_KEY = "amount";
    public static final String FREE_DOMAIN_PROMOTION = "free_domain";
    public static final String DOMAIN_DISCOUNT_RU_RF = "domain_discount_ru_rf";
    public static final String MAILING_TYPE_INFO = "info";
    public static final String MAILING_TYPE_TECH = "tech";
    public static final String MAILBOX_ANTISPAM_FIELD = "antiSpamEnabled";
    public static final String WEB_SITE_ID_KEY = "webSiteId";
    public static final String DATABASE_ID_KEY = "databaseId";
    public static final String DATABASE_USER_ID_KEY = "databaseUserId";
    public static final String DATABASE_USER_NAME_KEY = "databaseUserName";
    public static final String DATABASE_USER_PASSWORD_KEY = "databaseUserPassword";
    public static final String SERVER_ID_KEY = "serverId";
    public static final String WEBSITE_SERVICE_ID_KEY = "webSiteServiceId";
    public static final String WEBSITE_SERVER_NAME_KEY = "webSiteServerName";
    public static final String DATABASE_SERVICE_ID_KEY = "databaseServiceId";
    public static final String DOMAIN_ID_KEY = "domainId";
    public static final String DATABASE_HOST_KEY = "databaseHost";
    public static final String APP_ID_KEY = "appId";
    public static final String APPSCAT_DOMAIN_NAME_KEY = "DOMAIN_NAME";
    public static final String APPSCAT_APP_TITLE_KEY = "APP_TITLE";
    public static final String APPSCAT_APP_PATH_KEY = "APP_PATH";
    public static final String APPSCAT_ADMIN_USERNAME_KEY = "ADMIN_USERNAME";
    public static final String APPSCAT_ADMIN_PASSWORD_KEY = "ADMIN_PASSWORD";
    public static final String APPSCAT_DB_HOST_KEY = "DB_HOST";
    public static final String TRANSFER_DATABASES_KEY = "transferDatabases";
    public static final String SERVICE_ID_KEY = "serviceId";
    public static final String APPLICATION_SERVICE_ID_KEY = "applicationServiceId";
    public static final String OLD_UNIX_ACCOUNT_SERVER_ID_KEY = "oldUnixAccountServerId";
    public static final String NEW_UNIX_ACCOUNT_SERVER_ID_KEY = "newUnixAccountServerId";
    public static final String OLD_SERVER_NAME_KEY = "oldServerName";
    public static final String OLD_HTTP_PROXY_IP_KEY = "oldHttpProxyIp";
    public static final String NGINX_SERVICE_TEMPLATE_TYPE_NAME = "STAFF_NGINX";
    public static final String OLD_DATABASE_SERVER_ID_KEY = "oldDatabaseServerId";
    public static final String NEW_DATABASE_SERVER_ID_KEY = "newDatabaseServerId";
    public static final String OLD_WEBSITE_SERVER_ID_KEY = "oldWebSiteServerId";
    public static final String NEW_WEBSITE_SERVER_ID_KEY = "newWebSiteServerId";
    public static final String OLD_DATABASE_HOST_KEY = "oldDatabaseHost";
    public static final String NEW_DATABASE_HOST_KEY = "newDatabaseHost";
    public static final String DATA_KEY = "data";
    public static final String DOCUMENT_NUMBER_KEY = "documentNumber";
    public static final String DATASOURCE_URI_KEY = "datasourceUri";
    public static final String DATA_DESTINATION_URI_KEY = "datadestinationUri";
    public static final String DELETE_EXTRANEOUS_KEY = "deleteExtraneous";
    public static final String DATA_SOURCE_PARAMS_KEY = "dataSourceParams";
    public static final String TE_PARAMS_KEY = "teParams";
    public static final String PM_PARAM_PREFIX_KEY = "pmParam_";
    public static final String DATA_POSTPROCESSOR_ARGS_KEY = "dataPostprocessorArgs";
    public static final String DATA_POSTPROCESSOR_TYPE_KEY = "dataPostprocessorType";
    public static final String DATA_POSTPROCESSOR_STRING_REPLACE_ACTION = "string-replace";
    public static final String DATA_POSTPROCESSOR_STRING_SEARCH_PATTERN_ARG = "searchPattern";
    public static final String DATA_POSTPROCESSOR_STRING_REPLACE_STRING_ARG = "replaceString";
    public static final String DATA_POSTPROCESSOR_ERASER = "eraser";
    public static final String UNIX_ACCOUNT_AND_DATABASE_SENT_KEY = "unixAccountAndDatabaseSent";
    public static final String WAIT_FOR_DATABASE_UPDATE_KEY = "waitForDatabaseUpdate";
    public static final String WAIT_FOR_DATABASE_USER_UPDATE_KEY = "waitForDatabaseUserUpdate";
    public static final String DELAY_MESSAGE_KEY = "delayMessage";
    public static final String WEBSITE_SENT_KEY = "webSiteSent";
    public static final String DNS_RECORD_SENT_KEY = "dnsRecordSent";
    public static final String REVERTING_KEY = "reverting";
    public static final String FINISH_INSTALL_KEY = "finishInstall";
    public static final String SUCCESS_KEY = "success";
    public static final String SWITCHED_ON_KEY = "switchedOn";
    public static final String WILL_BE_DELETED_AFTER_KEY = "willBeDeletedAfter";
    public static final String APPSCAT_ROUTING_KEY = "appscat";
    public static final String MJ_PARENT_CLIENT_ID_IN_REGISTRANT = "2";
    public static final String LONG_LIFE = "longLife";
    public static final String RESOURCE_ARCHIVE_ID = "resourceArchiveId";
    public static final String RESOURCE_TYPE = "resourceType";
    public static final String PAYMENT_REDIRECT_PATH = "/payment-redirect";
    public static final String SSL_CHECK_PROHIBITED_KEY = "ssl_check_prohibited";

    public static final String WEBSITE_APACHE2_PERL = "WEBSITE_APACHE2_PERL*";
    public static final String WEBSITE_APACHE2_PHP = "WEBSITE_APACHE2_PHP*";
    public static final int DOMAIN_DISCOUNT_RU_RF_REGISTRATION_FREE_COUNT = 3;
    public static final int PLAN_PARKING_ID = 109;
    public static final String PLAN_PARKING_ID_STRING = "109";
    public static final int PLAN_PARKING_PLUS_ID = 124;
    public static final String PLAN_PARKING_PLUS_ID_STRING = "124";
    public static final int PLAN_UNLIMITED_ID = 9802;
    public static final String PLAN_UNLIMITED_SERVICE_ID = PLAN_SERVICE_PREFIX + PLAN_UNLIMITED_ID;
    public static final int PLAN_PARKING_DOMAINS_ID = 9803;
    public static final String PLAN_PARKING_DOMAINS_SERVICE_ID = PLAN_SERVICE_PREFIX + PLAN_PARKING_DOMAINS_ID;
    public static final int PLAN_START_ID = 9804;
    public static final int PLAN_UNLIMITED_PLUS_ID = 9805;
    public static final int PLAN_BUSINESS_ID = 9806;
    public static final int PLAN_BUSINESS_PLUS_ID = 9807;
    public static final String SITE_VISITKA_PLAN_OLD_ID = "9801";
    public static final String MAIL_PLAN_OLD_ID = "137";
    public static final String TECHNICAL_SUPPORT_EMAIL = "support@majordomo.ru";
    public static final Long PLAN_MIN_COST_TO_ORDER_ABONEMENT = 245L;

    public static final Integer DEFAULT_NOTIFY_DAYS = 14;

    public static final int ADDITIONAL_WEB_SITE_ID = 22;
    public static final String ADDITIONAL_WEB_SITE_SERVICE_ID = SERVICE_PREFIX + ADDITIONAL_WEB_SITE_ID;

    public static final int ADDITIONAL_FTP_ID = 4;
    public static final String ADDITIONAL_FTP_SERVICE_ID = SERVICE_PREFIX + ADDITIONAL_FTP_ID;
    public static final int ADDITIONAL_FTP_FREE_ID = 20;
    public static final String ADDITIONAL_FTP_FREE_SERVICE_ID = SERVICE_PREFIX + ADDITIONAL_FTP_FREE_ID;

    public static final String ORDER_DOCUMENT_PACKAGE_ID = "23";
    public static final String ORDER_DOCUMENT_PACKAGE_SERVICE_ID = SERVICE_PREFIX + ORDER_DOCUMENT_PACKAGE_ID;

    public static final int SMS_NOTIFICATIONS_10_RUB_ID = 18;
    public static final String SMS_NOTIFICATIONS_10_RUB_SERVICE_ID = SERVICE_PREFIX + SMS_NOTIFICATIONS_10_RUB_ID;
    public static final String SMS_NOTIFICATIONS_FREE_SERVICE_ID = SERVICE_PREFIX + SMS_NOTIFICATIONS_10_RUB_ID + FREE_SERVICE_POSTFIX;
    public static final int SMS_NOTIFICATIONS_29_RUB_ID = 21;
    public static final String SMS_NOTIFICATIONS_29_RUB_SERVICE_ID = SERVICE_PREFIX + SMS_NOTIFICATIONS_29_RUB_ID;

    public static final int ANTI_SPAM_ID = 13;
    public static final String ANTI_SPAM_SERVICE_ID = SERVICE_PREFIX + ANTI_SPAM_ID;

    private static final String REVISIUM_ID = "revisium";
    public static final String REVISIUM_SERVICE_ID = SERVICE_PREFIX + REVISIUM_ID;

    private static final String LONG_LIFE_RESOURCE_ARCHIVE_ID = "long_life_resource_archive";
    public static final String LONG_LIFE_RESOURCE_ARCHIVE_SERVICE_ID = SERVICE_PREFIX + LONG_LIFE_RESOURCE_ARCHIVE_ID;

    private static final String ADVANCED_BACKUP_ID = "advanced_backup";
    public static final String ADVANCED_BACKUP_SERVICE_ID = SERVICE_PREFIX + ADVANCED_BACKUP_ID;

    public static final String REDIRECT_SERVICE_OLD_ID = SERVICE_PREFIX + "redirect";
    public static final String ACCESS_TO_CONTROL_PANEL_SERVICE_OLD_ID = SERVICE_PREFIX + "access_to_control_panel";

    //Id услуги Доп.место в BillingDB
    public static final int ADDITIONAL_QUOTA_100_ID = 15;
    //Id услуги Доп.место
    public static final String ADDITIONAL_QUOTA_100_SERVICE_ID = SERVICE_PREFIX + ADDITIONAL_QUOTA_100_ID;
    //Размер одной услуги Доп.место 100Мб
    public static final long ADDITIONAL_QUOTA_100_CAPACITY = 102400L;

    public static final String ADDITIONAL_QUOTA_5K_ID = "additional_quota_5K";
    public static final String ADDITIONAL_QUOTA_5K_SERVICE_ID = SERVICE_PREFIX + ADDITIONAL_QUOTA_5K_ID;
    public static final long ADDITIONAL_QUOTA_5K_CAPACITY = 5242880L;

    //Значение Limit в тарифе, обозначающее "безлимитность"
    public static final int PLAN_PROPERTY_LIMIT_UNLIMITED = -1;

    public static final Map<String, DomainCategory> DOMAIN_CATEGORY_MAP = new HashMap<>();
    public static final Map<Integer, DomainRegistrar> DOMAIN_REGISTRAR_MAP = new HashMap<>();
    public static final Map<String, DomainRegistrar> DOMAIN_REGISTRAR_STRING_MAP = new HashMap<String, DomainRegistrar>();
    public static final Map<Integer, String> DOMAIN_REGISTRATOR_NAME_MAP = new HashMap<>();

    //Id услуг не нужных при импорте
    public static final Set<Integer> NOT_NEEDED_SERVICE_IDS = new HashSet<>();
    //При импорте "услуг аккаунта" не нужны некоторые услуги
    public static final Set<Integer> NOT_NEEDED_ACCOUNT_SERVICE_IDS = new HashSet<>();
    //Услуги которые могут быть бесплатными
    public static final Set<Integer> OPTIONALLY_FREE_SERVICE_IDS = new HashSet<>();

    public static final Long[] DAYS_FOR_ABONEMENT_EXPIRED_EMAIL_SEND = {13L, 7L, 5L, 3L, 2L, 1L};
    public static final Long[] DAYS_FOR_SERVICE_ABONEMENT_EXPIRED_EMAIL_SEND = {5L, 3L, 2L, 1L};
    public static final Long[] DAYS_FOR_ABONEMENT_EXPIRED_SMS_SEND = {5L, 1L};

    public static final String PARTNER_PROMOCODE_ACTION_ID = "57f3c4b8038d8a6054409853";
    public static final String BONUS_FREE_DOMAIN_PROMOCODE_ACTION_ID = "57f3c4b8038d8a6054409854";
    public static final String BONUS_UNLIMITED_3_M_PROMOCODE_ACTION_ID = "57f3c4b8038d8a6054409855";
    public static final String BONUS_UNLIMITED_1_M_PROMOCODE_ACTION_ID = "57f3c4b8038d8a6054409856";
    public static final String BONUS_PARKING_3_M_PROMOCODE_ACTION_ID = "57f3c4b8038d8a6054409857";
    public static final String DOMAIN_DISCOUNT_RU_RF_ACTION_ID = "57f3c4b8038d8a6054409858";
    public static final String PARTNER_CHECKOUT_SERVICE_ID = "590745d8719fca09b485524c";
    public static final String TOCHKA_BANK_PROMOCODE_TAG_ID = "5c01593cd003d109804ebdcd";
    public static final Long PARTNER_CHECKOUT_MIN_SUMM = 1500L;

    public static final Map<Integer, MailManagerMessageType> MANAGER_MESSAGE_TYPE_MAP = new HashMap<>();

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

        DOMAIN_REGISTRAR_MAP.put(1, DomainRegistrar.NETHOUSE);
        DOMAIN_REGISTRAR_MAP.put(2, DomainRegistrar.R01);
        DOMAIN_REGISTRAR_MAP.put(3, DomainRegistrar.RUCENTER);
        DOMAIN_REGISTRAR_MAP.put(4, DomainRegistrar.ENOM);
        DOMAIN_REGISTRAR_MAP.put(5, DomainRegistrar.GODADDY);
        DOMAIN_REGISTRAR_MAP.put(6, DomainRegistrar.GANDI);
        DOMAIN_REGISTRAR_MAP.put(7, DomainRegistrar.UKRNAMES);
        DOMAIN_REGISTRAR_MAP.put(8, DomainRegistrar.REGRU);
        DOMAIN_REGISTRAR_MAP.put(9, DomainRegistrar.WEBNAMES);

        DOMAIN_REGISTRAR_STRING_MAP.put("Registrant", DomainRegistrar.NETHOUSE);
        DOMAIN_REGISTRAR_STRING_MAP.put("GPT", DomainRegistrar.R01);
        DOMAIN_REGISTRAR_STRING_MAP.put("RUCENTER", DomainRegistrar.RUCENTER);
        DOMAIN_REGISTRAR_STRING_MAP.put("Enom", DomainRegistrar.ENOM);
        DOMAIN_REGISTRAR_STRING_MAP.put("GoDaddy", DomainRegistrar.GODADDY);
        DOMAIN_REGISTRAR_STRING_MAP.put("Ukrnames", DomainRegistrar.UKRNAMES);
        DOMAIN_REGISTRAR_STRING_MAP.put("RegRu", DomainRegistrar.REGRU);
        DOMAIN_REGISTRAR_STRING_MAP.put("Webnames", DomainRegistrar.WEBNAMES);

        DOMAIN_REGISTRATOR_NAME_MAP.put(1, "Nethouse");
        DOMAIN_REGISTRATOR_NAME_MAP.put(2, "R01");
        DOMAIN_REGISTRATOR_NAME_MAP.put(3, "RU-CENTER");
        DOMAIN_REGISTRATOR_NAME_MAP.put(4, "Enom");
        DOMAIN_REGISTRATOR_NAME_MAP.put(5, "GoDaddy");
        DOMAIN_REGISTRATOR_NAME_MAP.put(6, "Gandi");
        DOMAIN_REGISTRATOR_NAME_MAP.put(7, "Ukrnames");
        DOMAIN_REGISTRATOR_NAME_MAP.put(8, "РЕГ.РУ");
        DOMAIN_REGISTRATOR_NAME_MAP.put(9, "Webnames");

        NOT_NEEDED_SERVICE_IDS.add(2);
//        NOT_NEEDED_SERVICE_IDS.add(3);
        NOT_NEEDED_SERVICE_IDS.add(5);
        NOT_NEEDED_SERVICE_IDS.add(7);
        NOT_NEEDED_SERVICE_IDS.add(8);
        NOT_NEEDED_SERVICE_IDS.add(10);
        NOT_NEEDED_SERVICE_IDS.add(11);
//        NOT_NEEDED_SERVICE_IDS.add(14);
        NOT_NEEDED_SERVICE_IDS.add(16);
//        NOT_NEEDED_SERVICE_IDS.add(17);
        NOT_NEEDED_SERVICE_IDS.add(19);
        NOT_NEEDED_SERVICE_IDS.add(20);
//        NOT_NEEDED_SERVICE_IDS.add(21);

        NOT_NEEDED_ACCOUNT_SERVICE_IDS.addAll(NOT_NEEDED_SERVICE_IDS);
        //"Тарифный план (хостинг)"
        NOT_NEEDED_ACCOUNT_SERVICE_IDS.add(1);
        //Доменный алиас
        NOT_NEEDED_ACCOUNT_SERVICE_IDS.add(2);
        //Дополнительный домен
        NOT_NEEDED_ACCOUNT_SERVICE_IDS.add(3);

        //ДопFTP
        OPTIONALLY_FREE_SERVICE_IDS.add(4);
        //Защита от спама
        OPTIONALLY_FREE_SERVICE_IDS.add(13);
        //Дополнительный Webalizer
        OPTIONALLY_FREE_SERVICE_IDS.add(14);
        //СМС-уведомления
        OPTIONALLY_FREE_SERVICE_IDS.add(18);
    }

    public static final Map<String, BigDecimal> ACTION_DOMAINS_REGISTRATION_COST;
    static {
        Map<String, BigDecimal> temp = new HashMap<>();
        temp.put("shop", new BigDecimal(250L));
        temp.put("club", new BigDecimal(145L));
        ACTION_DOMAINS_REGISTRATION_COST = Collections.unmodifiableMap(temp);
    }

    public static final Map<String, BigDecimal> ACTION_DOMAINS_RENEW_COST;
    static {
        Map<String, BigDecimal> temp = new HashMap<>();
        temp.put("shop", new BigDecimal(3500L));
        temp.put("club", new BigDecimal(1500L));
        ACTION_DOMAINS_RENEW_COST = Collections.unmodifiableMap(temp);
    }

    public static final String ACTION_BITRIX_END_DATE = "2020-05-30 00:00:00";

    public static final String ACTION_BONUS_PAYMENT_BILL_START_DATE = "2020-05-07 00:00:00";
    public static final String ACTION_BONUS_PAYMENT_BILL_END_DATE = "2020-05-15 12:00:00";
    public static final String ACTION_BONUS_PAYMENT_BILL_ID = "24";

    public static class Exchanges {
        public static final String ACCOUNT_CREATE = "account.create";
        public static final String ACCOUNT_UPDATE = "account.update";
        public static final String ACCOUNT_DELETE = "account.delete";

        public static final String ACCOUNT_HISTORY = "account-history";

        public static final String DATABASE_CREATE = "database.create";
        public static final String DATABASE_UPDATE = "database.update";
        public static final String DATABASE_DELETE = "database.delete";

        public static final String DATABASE_USER_CREATE = "database-user.create";
        public static final String DATABASE_USER_UPDATE = "database-user.update";
        public static final String DATABASE_USER_DELETE = "database-user.delete";

        public static final String DNS_RECORD_CREATE = "dns-record.create";
        public static final String DNS_RECORD_UPDATE = "dns-record.update";
        public static final String DNS_RECORD_DELETE = "dns-record.delete";

        public static final String DOMAIN_CREATE = "domain.create";
        public static final String DOMAIN_UPDATE = "domain.update";
        public static final String DOMAIN_DELETE = "domain.delete";
        public static final String DOMAIN_TRANSFER_SYNCHRONIZATION = "domain.transfer-synchronization";

        public static final String FTP_USER_CREATE = "ftp-user.create";
        public static final String FTP_USER_UPDATE = "ftp-user.update";
        public static final String FTP_USER_DELETE = "ftp-user.delete";

        public static final String MAILBOX_CREATE = "mailbox.create";
        public static final String MAILBOX_UPDATE = "mailbox.update";
        public static final String MAILBOX_DELETE = "mailbox.delete";

        public static final String PAYMENT_CREATE = "payment.create";

        public static final String PERSON_CREATE = "person.create";
        public static final String PERSON_UPDATE = "person.update";
        public static final String PERSON_DELETE = "person.delete";

        public static final String RESOURCE_ARCHIVE_CREATE = "resource-archive.create";
        public static final String RESOURCE_ARCHIVE_UPDATE = "resource-archive.update";
        public static final String RESOURCE_ARCHIVE_DELETE = "resource-archive.delete";

        public static final String SSL_CERTIFICATE_CREATE = "ssl-certificate.create";
        public static final String SSL_CERTIFICATE_UPDATE = "ssl-certificate.update";
        public static final String SSL_CERTIFICATE_DELETE = "ssl-certificate.delete";

        public static final String UNIX_ACCOUNT_CREATE = "unix-account.create";
        public static final String UNIX_ACCOUNT_UPDATE = "unix-account.update";
        public static final String UNIX_ACCOUNT_DELETE = "unix-account.delete";

        public static final String WEBSITE_CREATE = "website.create";
        public static final String WEBSITE_UPDATE = "website.update";
        public static final String WEBSITE_DELETE = "website.delete";

        public static final String REDIRECT_CREATE = "redirect.create";
        public static final String REDIRECT_UPDATE = "redirect.update";
        public static final String REDIRECT_DELETE = "redirect.delete";

        public static final String SERVICE_CREATE = "service.create";
        public static final String SERVICE_UPDATE = "service.update";
        public static final String SERVICE_DELETE = "service.delete";

        public static final String APPS_CAT_INSTALL = "appscat.install";

        public static Set<String> ALL_EXCHANGES;

        static {
            ALL_EXCHANGES = new HashSet<>(Arrays.asList(
                    ACCOUNT_CREATE,
                    ACCOUNT_UPDATE,
                    ACCOUNT_DELETE,
                    ACCOUNT_HISTORY,
                    DATABASE_CREATE,
                    DATABASE_UPDATE,
                    DATABASE_DELETE,
                    DATABASE_USER_CREATE,
                    DATABASE_USER_UPDATE,
                    DATABASE_USER_DELETE,
                    DNS_RECORD_CREATE,
                    DNS_RECORD_UPDATE,
                    DNS_RECORD_DELETE,
                    DOMAIN_CREATE,
                    DOMAIN_UPDATE,
                    DOMAIN_DELETE,
                    DOMAIN_TRANSFER_SYNCHRONIZATION,
                    FTP_USER_CREATE,
                    FTP_USER_UPDATE,
                    FTP_USER_DELETE,
                    MAILBOX_CREATE,
                    MAILBOX_UPDATE,
                    MAILBOX_DELETE,
                    PAYMENT_CREATE,
                    PERSON_CREATE,
                    PERSON_UPDATE,
                    PERSON_DELETE,
                    RESOURCE_ARCHIVE_CREATE,
                    RESOURCE_ARCHIVE_UPDATE,
                    RESOURCE_ARCHIVE_DELETE,
                    SSL_CERTIFICATE_CREATE,
                    SSL_CERTIFICATE_UPDATE,
                    SSL_CERTIFICATE_DELETE,
                    UNIX_ACCOUNT_CREATE,
                    UNIX_ACCOUNT_UPDATE,
                    UNIX_ACCOUNT_DELETE,
                    WEBSITE_CREATE,
                    WEBSITE_UPDATE,
                    WEBSITE_DELETE,
                    REDIRECT_CREATE,
                    REDIRECT_UPDATE,
                    REDIRECT_DELETE,
                    APPS_CAT_INSTALL,
                    SERVICE_CREATE,
                    SERVICE_UPDATE,
                    SERVICE_DELETE
            ));
        }
    }

    public final static List<String> BAD_WORD_PATTERNS = Arrays.asList(
            "FUCK",
            "CUNT",
            "WANK",
            "WANG",
            "PISS",
            "COCK",
            "SHIT",
            "TWAT",
            "TITS",
            "FART",
            "HELL",
            "MUFF",
            "DICK",
            "KNOB",
            "ARSE",
            "SHAG",
            "TOSS",
            "SLUT",
            "TURD",
            "SLAG",
            "CRAP",
            "POOP",
            "BUTT",
            "FECK",
            "BOOB",
            "JISM",
            "JIZZ",
            "PHAT",
            ".?HUI.?",
            ".?HYI.?"
    );
}
