package ru.majordomo.hms.personmgr.model.plan;

public enum Feature {
    VIRTUAL_HOSTING_PLAN,
    ADDITIONAL_SERVICE,
    DOMAIN_REGISTRATION,
    DOMAIN_RENEW,
    SEO,
    REVISIUM,
    SMS_NOTIFICATIONS,
    FTP_USER,
    DISK_QUOTA,
    ANTI_SPAM,
    MAILBOX,
    SSL_CERTIFICATE,
    WEB_SITE,
    DATABASE,
    DATABASE_USER,
    BUSINESS_SERVICES,
    DOCUMENT_PACKAGE_ORDER,
    ADDITIONAL_QUOTA_5K,
    REDIRECT,
    LONG_LIFE_RESOURCE_ARCHIVE,
    ADVANCED_BACKUP,
    ADVANCED_BACKUP_INSTANT_ACCESS;

    public boolean isOnlyOnePerAccount() {
        switch (this) {
            case VIRTUAL_HOSTING_PLAN:
            case SMS_NOTIFICATIONS:
            case ANTI_SPAM:
            case ADVANCED_BACKUP:
            case ADVANCED_BACKUP_INSTANT_ACCESS:
            case ADDITIONAL_QUOTA_5K:
                return true;
            default:
                return false;
        }
    }

    public boolean canUserDelete() {
        switch (this) {
            case VIRTUAL_HOSTING_PLAN:
                return false;
            default:
                return true;
        }
    }
}
