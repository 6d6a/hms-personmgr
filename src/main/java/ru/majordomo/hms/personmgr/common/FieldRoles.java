package ru.majordomo.hms.personmgr.common;

import java.util.HashMap;
import java.util.Map;

import static ru.majordomo.hms.personmgr.common.SecurityConstants.MAILBOX_MAIL_SPOOL_EDIT;
import static ru.majordomo.hms.personmgr.common.SecurityConstants.WEBSITE_DDOS_PROTECTION_EDIT;
import static ru.majordomo.hms.personmgr.common.SecurityConstants.MAILBOX_MAIL_FROM_ALLOWED_EDIT;
import static ru.majordomo.hms.personmgr.common.SecurityConstants.RESOURCE_SWITCHED_ON_EDIT;
import static ru.majordomo.hms.personmgr.common.SecurityConstants.UNIX_ACCOUNT_SENDMAIL_ALLOWED_EDIT;

public class FieldRoles {
    public static final Map<String, String> RESOURCE_SWITCHED_ON = new HashMap<>();
    public static final Map<String, String> MAILBOX_PATCH = new HashMap<>();
    public static final Map<String, String> WEB_SITE_PATCH = new HashMap<>();
    public static final Map<String, String> UNIX_ACCOUNT_PATCH = new HashMap<>();
    public static final Map<String, String> DNS_RECORD_PATCH = new HashMap<>();

    static {
        RESOURCE_SWITCHED_ON.put("switchedOn", RESOURCE_SWITCHED_ON_EDIT);

        MAILBOX_PATCH.put("mailFromAllowed", MAILBOX_MAIL_FROM_ALLOWED_EDIT);
        MAILBOX_PATCH.put("mailSpool", MAILBOX_MAIL_SPOOL_EDIT);

        WEB_SITE_PATCH.put("ddosProtection", WEBSITE_DDOS_PROTECTION_EDIT);
        WEB_SITE_PATCH.putAll(RESOURCE_SWITCHED_ON);

        UNIX_ACCOUNT_PATCH.put("sendmailAllowed", UNIX_ACCOUNT_SENDMAIL_ALLOWED_EDIT);
        UNIX_ACCOUNT_PATCH.putAll(RESOURCE_SWITCHED_ON);

        DNS_RECORD_PATCH.putAll(RESOURCE_SWITCHED_ON);
    }
}