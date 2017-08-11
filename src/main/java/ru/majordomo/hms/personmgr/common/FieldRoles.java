package ru.majordomo.hms.personmgr.common;

import java.util.HashMap;
import java.util.Map;

import static ru.majordomo.hms.personmgr.common.SecurityConstants.*;

public class FieldRoles {
    public static final Map<String, String> RESOURCE_SWITCHED_ON = new HashMap<>();
    public static final Map<String, String> MAILBOX_PATCH = new HashMap<>();
    public static final Map<String, String> MAILBOX_POST = new HashMap<>();
    public static final Map<String, String> WEB_SITE_PATCH = new HashMap<>();
    public static final Map<String, String> WEB_SITE_POST = new HashMap<>();
    public static final Map<String, String> DATABASE_PATCH = new HashMap<>();
    public static final Map<String, String> DATABASE_POST = new HashMap<>();
    public static final Map<String, String> UNIX_ACCOUNT_PATCH = new HashMap<>();
    public static final Map<String, String> UNIX_ACCOUNT_POST = new HashMap<>();
    public static final Map<String, String> DNS_RECORD_PATCH = new HashMap<>();
    public static final Map<String, String> RESOURCE_WRITABLE = new HashMap<>();

    static {
        RESOURCE_SWITCHED_ON.put("switchedOn", RESOURCE_SWITCHED_ON_EDIT);
        RESOURCE_WRITABLE.put("writable", RESOURCE_WRITABLE_EDIT);

        MAILBOX_PATCH.put("mailFromAllowed", MAILBOX_MAIL_FROM_ALLOWED_EDIT);
        MAILBOX_PATCH.put("mailSpool", MAILBOX_MAIL_SPOOL_EDIT);
        MAILBOX_PATCH.putAll(RESOURCE_SWITCHED_ON);
        MAILBOX_PATCH.putAll(RESOURCE_WRITABLE);

        MAILBOX_POST.putAll(MAILBOX_PATCH);

        WEB_SITE_PATCH.put("ddosProtection", WEBSITE_DDOS_PROTECTION_EDIT);
        WEB_SITE_PATCH.putAll(RESOURCE_SWITCHED_ON);

        WEB_SITE_POST.putAll(WEB_SITE_PATCH);

        DATABASE_PATCH.putAll(RESOURCE_SWITCHED_ON);
        DATABASE_PATCH.putAll(RESOURCE_WRITABLE);

        DATABASE_POST.putAll(DATABASE_PATCH);

        UNIX_ACCOUNT_PATCH.put("sendmailAllowed", UNIX_ACCOUNT_SENDMAIL_ALLOWED_EDIT);
        UNIX_ACCOUNT_PATCH.put("quota", UNIX_ACCOUNT_QUOTA_EDIT);
        UNIX_ACCOUNT_PATCH.putAll(RESOURCE_SWITCHED_ON);
        UNIX_ACCOUNT_PATCH.putAll(RESOURCE_WRITABLE);

        UNIX_ACCOUNT_POST.putAll(UNIX_ACCOUNT_PATCH);

        DNS_RECORD_PATCH.putAll(RESOURCE_SWITCHED_ON);
    }
}
