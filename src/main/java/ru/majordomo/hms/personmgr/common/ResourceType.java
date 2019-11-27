package ru.majordomo.hms.personmgr.common;

/**
 * По видимому, это должны быть ресурсы rc-user которые можно было запрещать через Plan.prohibitedResourceTypes
 */
public enum ResourceType {
    MAILBOX,
    SSL_CERTIFICATE,
    WEB_SITE,
    DATABASE,
    DATABASE_USER,
    DOMAIN,
    UNIX_ACCOUNT,
    FTP_USER,
    RESOURCE_ARCHIVE,
    /**
     * Выделенный сервис (такой как выделеный php или node.js), данный ресурс хранится в rc-staff
     */
    DEDICATED_APP_SERVICE
}
