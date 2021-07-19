package ru.majordomo.hms.personmgr.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import ru.majordomo.hms.personmgr.dto.AppscatApp;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.rc.staff.resources.Service;
import ru.majordomo.hms.rc.user.resources.*;

import javax.annotation.Nonnull;

@Getter
@RequiredArgsConstructor
public enum BusinessActionType {
    WEB_SITE_CREATE_RC(WebSite.class),
    WEB_SITE_UPDATE_RC(WebSite.class),
    WEB_SITE_DELETE_RC(WebSite.class),
    DATABASE_CREATE_RC(Database.class),
    DATABASE_UPDATE_RC(Database.class),
    DATABASE_DELETE_RC(Database.class),
    DATABASE_USER_CREATE_RC(DatabaseUser.class),
    DATABASE_USER_UPDATE_RC(DatabaseUser.class),
    DATABASE_USER_DELETE_RC(DatabaseUser.class),
    MAILBOX_CREATE_RC(Mailbox.class),
    MAILBOX_UPDATE_RC(Mailbox.class),
    MAILBOX_DELETE_RC(Mailbox.class),
    PERSON_CREATE_RC(Person.class),
    PERSON_UPDATE_RC(Person.class),
    PERSON_DELETE_RC(Person.class),
    DOMAIN_CREATE_RC(Domain.class),
    DOMAIN_UPDATE_RC(Domain.class),
    DOMAIN_DELETE_RC(Domain.class),
    ACCOUNT_CREATE_RC(UnixAccount.class),
    ACCOUNT_CREATE_SI(PersonalAccount.class),
    ACCOUNT_CREATE_FIN(PersonalAccount.class),
    ACCOUNT_UPDATE_RC(UnixAccount.class),
    ACCOUNT_DELETE_RC(UnixAccount.class),
    SSL_CERTIFICATE_CREATE_RC(SSLCertificate.class),
    SSL_CERTIFICATE_UPDATE_RC(SSLCertificate.class),
    SSL_CERTIFICATE_DELETE_RC(SSLCertificate.class),
    FTP_USER_CREATE_RC(FTPUser.class),
    FTP_USER_UPDATE_RC(FTPUser.class),
    FTP_USER_DELETE_RC(FTPUser.class),
    UNIX_ACCOUNT_CREATE_RC(UnixAccount.class),
    UNIX_ACCOUNT_UPDATE_RC(UnixAccount.class),
    UNIX_ACCOUNT_DELETE_RC(UnixAccount.class),
    DNS_RECORD_CREATE_RC(DNSResourceRecord.class),
    DNS_RECORD_UPDATE_RC(DNSResourceRecord.class),
    DNS_RECORD_DELETE_RC(DNSResourceRecord.class),
    RESOURCE_ARCHIVE_CREATE_RC(ResourceArchive.class),
    RESOURCE_ARCHIVE_UPDATE_RC(ResourceArchive.class),
    RESOURCE_ARCHIVE_DELETE_RC(ResourceArchive.class),
    APP_INSTALL_APPSCAT(AppscatApp.class),
    REDIRECT_CREATE_RC(Redirect.class),
    REDIRECT_UPDATE_RC(Redirect.class),
    REDIRECT_DELETE_RC(Redirect.class),
    FILE_BACKUP_RESTORE_TE(UnixAccount.class),
    DATABASE_RESTORE_TE(Database.class),
    DEDICATED_APP_SERVICE_CREATE_RC_STAFF(Service.class),
    DEDICATED_APP_SERVICE_UPDATE_RC_STAFF(Service.class),
    DEDICATED_APP_SERVICE_DELETE_RC_STAFF(Service.class);

    @Nonnull
    private final Class<?> resourceClass;
}
