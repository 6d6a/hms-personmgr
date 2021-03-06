package ru.majordomo.hms.personmgr.common;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static ru.majordomo.hms.personmgr.common.Constants.*;
import static ru.majordomo.hms.personmgr.common.SecurityConstants.*;

public class FieldRoles {
    public static final Map<String, String> RESOURCE_SWITCHED_ON = new HashMap<>();
    public static final Map<String, String> RESOURCE_WILL_BE_DELETED_AFTER = new HashMap<>();
    public static final Map<String, String> MAILBOX_PATCH = new HashMap<>();
    public static final Map<String, String> MAILBOX_POST = new HashMap<>();
    public static final Map<String, String> WEB_SITE_PATCH = new HashMap<>();
    public static final Map<String, String> WEB_SITE_POST = new HashMap<>();
    public static final Map<String, String> DATABASE_PATCH = new HashMap<>();
    public static final Map<String, String> DATABASE_POST = new HashMap<>();
    public static final Map<String, String> DATABASE_USER_PATCH = new HashMap<>();
    public static final Map<String, String> DATABASE_USER_POST = new HashMap<>();
    public static final Map<String, String> UNIX_ACCOUNT_PATCH = new HashMap<>();
    public static final Map<String, String> UNIX_ACCOUNT_POST = new HashMap<>();
    public static final Map<String, String> DNS_RECORD_PATCH = new HashMap<>();
    public static final Map<String, String> RESOURCE_WRITABLE = new HashMap<>();
    public static final Map<String, String> RESOURCE_SERVICE_ID = new HashMap<>();
    public static final Map<String, String> REDIRECT_POST = new HashMap<>();
    public static final Map<String, String> REDIRECT_PATCH = new HashMap<>();
    public static final Map<String, String> DEDICATED_APP_SERVER_POST = new HashMap<>();
    public static final Map<String, String> DEDICATED_APP_SERVER_PATCH = new HashMap<>();
    public static final Map<String, String> RESOURCE_TE_PARAMS = new HashMap<>();
    public static final Map<String, String> DOMAIN_PATCH = Collections.singletonMap("infested", RESOURCE_SWITCHED_ON_EDIT);

    static {
        RESOURCE_SWITCHED_ON.put("switchedOn", RESOURCE_SWITCHED_ON_EDIT);
        RESOURCE_WILL_BE_DELETED_AFTER.put(WILL_BE_DELETED_AFTER_KEY, RESOURCE_WILL_BE_DELETED_AFTER_EDIT);
        RESOURCE_WRITABLE.put("writable", RESOURCE_WRITABLE_EDIT);
        RESOURCE_SERVICE_ID.put("serviceId", TRANSFER_ACCOUNT);
        RESOURCE_TE_PARAMS.put(TE_PARAMS_KEY, TE_PARAMS_SEND_ALLOWED);

        REDIRECT_POST.putAll(RESOURCE_SWITCHED_ON);
        REDIRECT_POST.putAll(RESOURCE_WILL_BE_DELETED_AFTER);
        REDIRECT_POST.putAll(RESOURCE_SERVICE_ID);
        REDIRECT_POST.putAll(RESOURCE_TE_PARAMS);
        REDIRECT_PATCH.putAll(REDIRECT_POST);

        DEDICATED_APP_SERVER_POST.putAll(RESOURCE_SWITCHED_ON);
        DEDICATED_APP_SERVER_PATCH.putAll(DEDICATED_APP_SERVER_POST);

        MAILBOX_PATCH.put("mailFromAllowed", MAILBOX_MAIL_FROM_ALLOWED_EDIT);
        MAILBOX_PATCH.put("mailSpool", MAILBOX_MAIL_SPOOL_EDIT);
        MAILBOX_PATCH.putAll(RESOURCE_SWITCHED_ON);
        MAILBOX_PATCH.putAll(RESOURCE_WILL_BE_DELETED_AFTER);
        MAILBOX_PATCH.putAll(RESOURCE_WRITABLE);

        MAILBOX_POST.putAll(MAILBOX_PATCH);

        WEB_SITE_PATCH.put("ddosProtection", WEBSITE_DDOS_PROTECTION_EDIT);
        WEB_SITE_PATCH.putAll(RESOURCE_SWITCHED_ON);
        WEB_SITE_PATCH.putAll(RESOURCE_WILL_BE_DELETED_AFTER);
        WEB_SITE_PATCH.putAll(RESOURCE_TE_PARAMS);

        WEB_SITE_POST.putAll(WEB_SITE_PATCH);

        DATABASE_PATCH.putAll(RESOURCE_SWITCHED_ON);
        DATABASE_PATCH.putAll(RESOURCE_WILL_BE_DELETED_AFTER);
        DATABASE_PATCH.putAll(RESOURCE_WRITABLE);
        DATABASE_PATCH.putAll(RESOURCE_SERVICE_ID);
        DATABASE_PATCH.putAll(RESOURCE_TE_PARAMS);

        DATABASE_POST.putAll(RESOURCE_SWITCHED_ON);
        DATABASE_POST.putAll(RESOURCE_TE_PARAMS);
        DATABASE_POST.putAll(RESOURCE_WRITABLE);

        DATABASE_USER_PATCH.putAll(RESOURCE_SWITCHED_ON);
        DATABASE_USER_PATCH.putAll(RESOURCE_WILL_BE_DELETED_AFTER);
        DATABASE_USER_PATCH.putAll(RESOURCE_SERVICE_ID);
        DATABASE_USER_PATCH.putAll(RESOURCE_TE_PARAMS);
        DATABASE_USER_PATCH.put("maxCpuTimePerSecond", DATABASE_USER_MAX_CPU_TIME_PER_SECOND_EDIT);

        DATABASE_USER_POST.put("maxCpuTimePerSecond", DATABASE_USER_MAX_CPU_TIME_PER_SECOND_EDIT);
        DATABASE_USER_POST.putAll(RESOURCE_SWITCHED_ON);
        DATABASE_USER_POST.putAll(RESOURCE_TE_PARAMS);

        UNIX_ACCOUNT_PATCH.put("sendmailAllowed", UNIX_ACCOUNT_SENDMAIL_ALLOWED_EDIT);
        UNIX_ACCOUNT_PATCH.put("quota", UNIX_ACCOUNT_QUOTA_EDIT);
        UNIX_ACCOUNT_PATCH.put(SERVER_ID_KEY, TRANSFER_ACCOUNT);
        UNIX_ACCOUNT_PATCH.putAll(RESOURCE_SWITCHED_ON);
        UNIX_ACCOUNT_PATCH.putAll(RESOURCE_WILL_BE_DELETED_AFTER);
        UNIX_ACCOUNT_PATCH.putAll(RESOURCE_WRITABLE);
        UNIX_ACCOUNT_PATCH.putAll(RESOURCE_TE_PARAMS);

        UNIX_ACCOUNT_POST.putAll(UNIX_ACCOUNT_PATCH);

        DNS_RECORD_PATCH.putAll(RESOURCE_SWITCHED_ON);
    }
}
