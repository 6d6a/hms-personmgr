package ru.majordomo.hms.personmgr.service.importing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

import ru.majordomo.hms.personmgr.common.BusinessActionType;
import ru.majordomo.hms.personmgr.common.message.destination.MailManagerMessageDestination;
import ru.majordomo.hms.personmgr.common.message.destination.AmqpMessageDestination;
import ru.majordomo.hms.personmgr.model.BusinessAction;
import ru.majordomo.hms.personmgr.repository.BusinessActionRepository;

/**
 * Сервис для загрузки первичных данных в БД
 */
@Service
public class BusinessActionDBSeedService {
    private final static Logger logger = LoggerFactory.getLogger(BusinessActionDBSeedService.class);

    @Autowired
    private BusinessActionRepository businessActionRepository;

    @Value( "${mail_manager.dev_email}" )
    private String mailManagerDevEmail;

    public boolean seedDB() {
        boolean result = false;

        businessActionRepository.deleteAll();

        this.seedWebSiteCreateActions();
        this.seedWebSiteUpdateActions();
        this.seedWebSiteDeleteActions();

        this.seedDatabaseCreateActions();
        this.seedDatabaseUpdateActions();
        this.seedDatabaseDeleteActions();

        this.seedMailboxCreateActions();
        this.seedMailboxUpdateActions();
        this.seedMailboxDeleteActions();

        this.seedDatabaseUserCreateActions();
        this.seedDatabaseUserUpdateActions();
        this.seedDatabaseUserDeleteActions();

        this.seedPersonCreateActions();
        this.seedPersonUpdateActions();
        this.seedPersonDeleteActions();

        this.seedDomainCreateActions();
        this.seedDomainUpdateActions();
        this.seedDomainDeleteActions();

        this.seedAccountCreateActions();
        this.seedAccountUpdateActions();
        this.seedAccountDeleteActions();

        this.seedSslCertificateCreateActions();
        this.seedSslCertificateUpdateActions();
        this.seedSslCertificateDeleteActions();

        this.seedFtpUserCreateActions();
        this.seedFtpUserUpdateActions();
        this.seedFtpUserDeleteActions();

        this.seedUnixAccountCreateActions();
        this.seedUnixAccountUpdateActions();
        this.seedUnixAccountDeleteActions();

        this.seedSeoOrderActions();

        logger.debug("BusinessAction found with findAll():");
        logger.debug("-------------------------------");

        List<BusinessAction> businessActions = businessActionRepository.findAll();
        if (businessActions.size() > 0) {
            result = true;
        }
        for (BusinessAction businessAction : businessActions) {
            logger.debug("businessAction: " + businessAction.toString());
        }

        return result;
    }

    private void seedWebSiteCreateActions() {
        BusinessAction action;
        AmqpMessageDestination amqpMessageDestination;
        MailManagerMessageDestination mailManagerMessageDestination;

        //WebSite create
        action = new BusinessAction();
        action.setBusinessActionType(BusinessActionType.WEB_SITE_CREATE_RC);
        action.setName("WebSite create RC");

        amqpMessageDestination = new AmqpMessageDestination();
        amqpMessageDestination.setExchange("website.create");
        amqpMessageDestination.setRoutingKey("rc.user");

        action.setDestination(amqpMessageDestination);

        action.setPriority(1);

        businessActionRepository.save(action);

        action = new BusinessAction();
        action.setBusinessActionType(BusinessActionType.WEB_SITE_CREATE_MM);
        action.setName("WebSite create MM");

        mailManagerMessageDestination = new MailManagerMessageDestination();

        action.setDestination(mailManagerMessageDestination);

        action.setPriority(2);

        businessActionRepository.save(action);
    }

    private void seedWebSiteUpdateActions() {
        BusinessAction action;
        AmqpMessageDestination amqpMessageDestination;

        //WebSite update
        action = new BusinessAction();
        action.setBusinessActionType(BusinessActionType.WEB_SITE_UPDATE_RC);
        action.setName("WebSite update");

        amqpMessageDestination = new AmqpMessageDestination();
        amqpMessageDestination.setExchange("website.update");
        amqpMessageDestination.setRoutingKey("rc.user");

        action.setDestination(amqpMessageDestination);

        action.setPriority(1);

        businessActionRepository.save(action);
    }

    private void seedWebSiteDeleteActions() {
        BusinessAction action;
        AmqpMessageDestination amqpMessageDestination;

        //WebSite delete
        action = new BusinessAction();
        action.setBusinessActionType(BusinessActionType.WEB_SITE_DELETE_RC);
        action.setName("WebSite delete");

        amqpMessageDestination = new AmqpMessageDestination();
        amqpMessageDestination.setExchange("website.delete");
        amqpMessageDestination.setRoutingKey("rc.user");

        action.setDestination(amqpMessageDestination);

        action.setPriority(1);

        businessActionRepository.save(action);
    }

    private void seedDatabaseCreateActions() {
        BusinessAction action;
        AmqpMessageDestination amqpMessageDestination;

        //Database create
        action = new BusinessAction();
        action.setBusinessActionType(BusinessActionType.DATABASE_CREATE_RC);
        action.setName("Database create");

        amqpMessageDestination = new AmqpMessageDestination();
        amqpMessageDestination.setExchange("database.create");
        amqpMessageDestination.setRoutingKey("rc.user");

        action.setDestination(amqpMessageDestination);

        businessActionRepository.save(action);
    }

    private void seedDatabaseUpdateActions() {
        BusinessAction action;
        AmqpMessageDestination amqpMessageDestination;

        //Database update
        action = new BusinessAction();
        action.setBusinessActionType(BusinessActionType.DATABASE_UPDATE_RC);
        action.setName("Database update");

        amqpMessageDestination = new AmqpMessageDestination();
        amqpMessageDestination.setExchange("database.update");
        amqpMessageDestination.setRoutingKey("rc.user");

        action.setDestination(amqpMessageDestination);

        businessActionRepository.save(action);
    }

    private void seedDatabaseDeleteActions() {
        BusinessAction action;
        AmqpMessageDestination amqpMessageDestination;

        //Database delete
        action = new BusinessAction();
        action.setBusinessActionType(BusinessActionType.DATABASE_DELETE_RC);
        action.setName("Database delete");

        amqpMessageDestination = new AmqpMessageDestination();
        amqpMessageDestination.setExchange("database.delete");
        amqpMessageDestination.setRoutingKey("rc.user");

        action.setDestination(amqpMessageDestination);

        action.setPriority(1);

        businessActionRepository.save(action);
    }

    private void seedMailboxCreateActions() {
        BusinessAction action;
        AmqpMessageDestination amqpMessageDestination;

        //Mailbox create
        action = new BusinessAction();
        action.setBusinessActionType(BusinessActionType.MAILBOX_CREATE_RC);
        action.setName("Mailbox create");

        amqpMessageDestination = new AmqpMessageDestination();
        amqpMessageDestination.setExchange("mailbox.create");
        amqpMessageDestination.setRoutingKey("rc.user");

        action.setDestination(amqpMessageDestination);

        businessActionRepository.save(action);
    }

    private void seedMailboxUpdateActions() {
        BusinessAction action;
        AmqpMessageDestination amqpMessageDestination;

        //Mailbox update
        action = new BusinessAction();
        action.setBusinessActionType(BusinessActionType.MAILBOX_UPDATE_RC);
        action.setName("Mailbox update");

        amqpMessageDestination = new AmqpMessageDestination();
        amqpMessageDestination.setExchange("mailbox.update");
        amqpMessageDestination.setRoutingKey("rc.user");

        action.setDestination(amqpMessageDestination);

        businessActionRepository.save(action);
    }

    private void seedMailboxDeleteActions() {
        BusinessAction action;
        AmqpMessageDestination amqpMessageDestination;

        //Mailbox delete
        action = new BusinessAction();
        action.setBusinessActionType(BusinessActionType.MAILBOX_DELETE_RC);
        action.setName("Mailbox delete");

        amqpMessageDestination = new AmqpMessageDestination();
        amqpMessageDestination.setExchange("mailbox.delete");
        amqpMessageDestination.setRoutingKey("rc.user");

        action.setDestination(amqpMessageDestination);

        action.setPriority(1);

        businessActionRepository.save(action);
    }

    private void seedDatabaseUserCreateActions() {
        BusinessAction action;
        AmqpMessageDestination amqpMessageDestination;

        //Database User create
        action = new BusinessAction();
        action.setBusinessActionType(BusinessActionType.DATABASE_USER_CREATE_RC);
        action.setName("Database User create");

        amqpMessageDestination = new AmqpMessageDestination();
        amqpMessageDestination.setExchange("database-user.create");
        amqpMessageDestination.setRoutingKey("rc.user");

        action.setDestination(amqpMessageDestination);

        businessActionRepository.save(action);
    }

    private void seedDatabaseUserUpdateActions() {
        BusinessAction action;
        AmqpMessageDestination amqpMessageDestination;

        //Database User update
        action = new BusinessAction();
        action.setBusinessActionType(BusinessActionType.DATABASE_USER_UPDATE_RC);
        action.setName("Database User update");

        amqpMessageDestination = new AmqpMessageDestination();
        amqpMessageDestination.setExchange("database-user.update");
        amqpMessageDestination.setRoutingKey("rc.user");

        action.setDestination(amqpMessageDestination);

        businessActionRepository.save(action);
    }

    private void seedDatabaseUserDeleteActions() {
        BusinessAction action;
        AmqpMessageDestination amqpMessageDestination;

        //Database User delete
        action = new BusinessAction();
        action.setBusinessActionType(BusinessActionType.DATABASE_USER_DELETE_RC);
        action.setName("Database User delete");

        amqpMessageDestination = new AmqpMessageDestination();
        amqpMessageDestination.setExchange("database-user.delete");
        amqpMessageDestination.setRoutingKey("rc.user");

        action.setDestination(amqpMessageDestination);

        action.setPriority(1);

        businessActionRepository.save(action);
    }

    private void seedPersonCreateActions() {
        BusinessAction action;
        AmqpMessageDestination amqpMessageDestination;


        //Person create
        action = new BusinessAction();
        action.setBusinessActionType(BusinessActionType.PERSON_CREATE_RC);
        action.setName("Person create");

        amqpMessageDestination = new AmqpMessageDestination();
        amqpMessageDestination.setExchange("person.create");
        amqpMessageDestination.setRoutingKey("rc.user");

        action.setDestination(amqpMessageDestination);

        businessActionRepository.save(action);
    }

    private void seedPersonUpdateActions() {
        BusinessAction action;
        AmqpMessageDestination amqpMessageDestination;

        //Person update
        action = new BusinessAction();
        action.setBusinessActionType(BusinessActionType.PERSON_UPDATE_RC);
        action.setName("Person update");

        amqpMessageDestination = new AmqpMessageDestination();
        amqpMessageDestination.setExchange("person.update");
        amqpMessageDestination.setRoutingKey("rc.user");

        action.setDestination(amqpMessageDestination);

        businessActionRepository.save(action);
    }

    private void seedPersonDeleteActions() {
        BusinessAction action;
        AmqpMessageDestination amqpMessageDestination;

        //Person delete
        action = new BusinessAction();
        action.setBusinessActionType(BusinessActionType.PERSON_DELETE_RC);
        action.setName("Person delete");

        amqpMessageDestination = new AmqpMessageDestination();
        amqpMessageDestination.setExchange("person.delete");
        amqpMessageDestination.setRoutingKey("rc.user");

        action.setDestination(amqpMessageDestination);

        action.setPriority(1);

        businessActionRepository.save(action);
    }

    private void seedDomainCreateActions() {
        BusinessAction action;
        AmqpMessageDestination amqpMessageDestination;

        //Domain create
        action = new BusinessAction();
        action.setBusinessActionType(BusinessActionType.DOMAIN_CREATE_RC);
        action.setName("Domain create");

        amqpMessageDestination = new AmqpMessageDestination();
        amqpMessageDestination.setExchange("domain.create");
        amqpMessageDestination.setRoutingKey("rc.user");

        action.setDestination(amqpMessageDestination);

        businessActionRepository.save(action);
    }

    private void seedDomainUpdateActions() {
        BusinessAction action;
        AmqpMessageDestination amqpMessageDestination;

        //Domain update
        action = new BusinessAction();
        action.setBusinessActionType(BusinessActionType.DOMAIN_UPDATE_RC);
        action.setName("Domain update");

        amqpMessageDestination = new AmqpMessageDestination();
        amqpMessageDestination.setExchange("domain.update");
        amqpMessageDestination.setRoutingKey("rc.user");

        action.setDestination(amqpMessageDestination);

        businessActionRepository.save(action);
    }

    private void seedDomainDeleteActions() {
        BusinessAction action;
        AmqpMessageDestination amqpMessageDestination;

        //Domain delete
        action = new BusinessAction();
        action.setBusinessActionType(BusinessActionType.DOMAIN_DELETE_RC);
        action.setName("Domain delete");

        amqpMessageDestination = new AmqpMessageDestination();
        amqpMessageDestination.setExchange("domain.delete");
        amqpMessageDestination.setRoutingKey("rc.user");

        action.setDestination(amqpMessageDestination);

        action.setPriority(1);

        businessActionRepository.save(action);
    }

    private void seedAccountCreateActions() {
        BusinessAction action;
        AmqpMessageDestination amqpMessageDestination;
        MailManagerMessageDestination mailManagerMessageDestination;

        //Account create
        action = new BusinessAction();
        action.setBusinessActionType(BusinessActionType.ACCOUNT_CREATE_SI);
        action.setName("Account create SI");

        amqpMessageDestination = new AmqpMessageDestination();
        amqpMessageDestination.setExchange("account.create");
        amqpMessageDestination.setRoutingKey("si");

        action.setDestination(amqpMessageDestination);

        action.setPriority(1);

        businessActionRepository.save(action);

        action = new BusinessAction();
        action.setBusinessActionType(BusinessActionType.ACCOUNT_CREATE_FIN);
        action.setName("Account create FIN");

        amqpMessageDestination = new AmqpMessageDestination();
        amqpMessageDestination.setExchange("account.create");
        amqpMessageDestination.setRoutingKey("fin");

        action.setDestination(amqpMessageDestination);

        action.setPriority(2);

        businessActionRepository.save(action);

        action = new BusinessAction();
        action.setBusinessActionType(BusinessActionType.ACCOUNT_CREATE_RC);
        action.setName("Account create");

        amqpMessageDestination = new AmqpMessageDestination();
        amqpMessageDestination.setExchange("account.create");
        amqpMessageDestination.setRoutingKey("rc.user");

        action.setDestination(amqpMessageDestination);

        action.setPriority(3);

        businessActionRepository.save(action);

        action = new BusinessAction();
        action.setBusinessActionType(BusinessActionType.ACCOUNT_CREATE_MM);
        action.setName("Account create MM");

        mailManagerMessageDestination = new MailManagerMessageDestination();

        action.setDestination(mailManagerMessageDestination);

        action.setPriority(4);

        businessActionRepository.save(action);
    }

    private void seedAccountUpdateActions() {
        BusinessAction action;
        AmqpMessageDestination amqpMessageDestination;

        //Account update
        action = new BusinessAction();
        action.setBusinessActionType(BusinessActionType.ACCOUNT_UPDATE_RC);
        action.setName("Account update");

        amqpMessageDestination = new AmqpMessageDestination();
        amqpMessageDestination.setExchange("account.update");
        amqpMessageDestination.setRoutingKey("rc.user");

        action.setDestination(amqpMessageDestination);

        businessActionRepository.save(action);
    }

    private void seedAccountDeleteActions() {
        BusinessAction action;
        AmqpMessageDestination amqpMessageDestination;

        //Account delete
        action = new BusinessAction();
        action.setBusinessActionType(BusinessActionType.ACCOUNT_DELETE_RC);
        action.setName("Account delete");

        amqpMessageDestination = new AmqpMessageDestination();
        amqpMessageDestination.setExchange("account.delete");
        amqpMessageDestination.setRoutingKey("rc.user");

        action.setDestination(amqpMessageDestination);

        action.setPriority(1);

        businessActionRepository.save(action);
    }

    private void seedSslCertificateCreateActions() {
        BusinessAction action;
        AmqpMessageDestination amqpMessageDestination;

        //SslCertificate create
        action = new BusinessAction();
        action.setBusinessActionType(BusinessActionType.SSL_CERTIFICATE_CREATE_RC);
        action.setName("SslCertificate create");

        amqpMessageDestination = new AmqpMessageDestination();
        amqpMessageDestination.setExchange("ssl-certificate.create");
        amqpMessageDestination.setRoutingKey("rc.user");

        action.setDestination(amqpMessageDestination);

        businessActionRepository.save(action);
    }

    private void seedSslCertificateUpdateActions() {
        BusinessAction action;
        AmqpMessageDestination amqpMessageDestination;

        //SslCertificate update
        action = new BusinessAction();
        action.setBusinessActionType(BusinessActionType.SSL_CERTIFICATE_UPDATE_RC);
        action.setName("SslCertificate update");

        amqpMessageDestination = new AmqpMessageDestination();
        amqpMessageDestination.setExchange("ssl-certificate.update");
        amqpMessageDestination.setRoutingKey("rc.user");

        action.setDestination(amqpMessageDestination);

        businessActionRepository.save(action);
    }

    private void seedSslCertificateDeleteActions() {
        BusinessAction action;
        AmqpMessageDestination amqpMessageDestination;

        //SslCertificate delete
        action = new BusinessAction();
        action.setBusinessActionType(BusinessActionType.SSL_CERTIFICATE_DELETE_RC);
        action.setName("SslCertificate delete");

        amqpMessageDestination = new AmqpMessageDestination();
        amqpMessageDestination.setExchange("ssl-certificate.delete");
        amqpMessageDestination.setRoutingKey("rc.user");

        action.setDestination(amqpMessageDestination);

        action.setPriority(1);

        businessActionRepository.save(action);
    }

    private void seedFtpUserCreateActions() {
        BusinessAction action;
        AmqpMessageDestination amqpMessageDestination;

        //FtpUser create
        action = new BusinessAction();
        action.setBusinessActionType(BusinessActionType.FTP_USER_CREATE_RC);
        action.setName("FtpUser create");

        amqpMessageDestination = new AmqpMessageDestination();
        amqpMessageDestination.setExchange("ftp-user.create");
        amqpMessageDestination.setRoutingKey("rc.user");

        action.setDestination(amqpMessageDestination);

        businessActionRepository.save(action);
    }

    private void seedFtpUserUpdateActions() {
        BusinessAction action;
        AmqpMessageDestination amqpMessageDestination;

        //FtpUser update
        action = new BusinessAction();
        action.setBusinessActionType(BusinessActionType.FTP_USER_UPDATE_RC);
        action.setName("FtpUser update");

        amqpMessageDestination = new AmqpMessageDestination();
        amqpMessageDestination.setExchange("ftp-user.update");
        amqpMessageDestination.setRoutingKey("rc.user");

        action.setDestination(amqpMessageDestination);

        businessActionRepository.save(action);
    }

    private void seedFtpUserDeleteActions() {
        BusinessAction action;
        AmqpMessageDestination amqpMessageDestination;

        //FtpUser delete
        action = new BusinessAction();
        action.setBusinessActionType(BusinessActionType.FTP_USER_DELETE_RC);
        action.setName("FtpUser delete");

        amqpMessageDestination = new AmqpMessageDestination();
        amqpMessageDestination.setExchange("ftp-user.delete");
        amqpMessageDestination.setRoutingKey("rc.user");

        action.setDestination(amqpMessageDestination);

        action.setPriority(1);

        businessActionRepository.save(action);
    }

    private void seedUnixAccountCreateActions() {
        BusinessAction action;
        AmqpMessageDestination amqpMessageDestination;

        //UnixAccount create
        action = new BusinessAction();
        action.setBusinessActionType(BusinessActionType.UNIX_ACCOUNT_CREATE_RC);
        action.setName("UnixAccount create");

        amqpMessageDestination = new AmqpMessageDestination();
        amqpMessageDestination.setExchange("unix-account.create");
        amqpMessageDestination.setRoutingKey("rc.user");

        action.setDestination(amqpMessageDestination);

        businessActionRepository.save(action);
    }

    private void seedUnixAccountUpdateActions() {
        BusinessAction action;
        AmqpMessageDestination amqpMessageDestination;

        //UnixAccount update
        action = new BusinessAction();
        action.setBusinessActionType(BusinessActionType.UNIX_ACCOUNT_UPDATE_RC);
        action.setName("UnixAccount update");

        amqpMessageDestination = new AmqpMessageDestination();
        amqpMessageDestination.setExchange("unix-account.update");
        amqpMessageDestination.setRoutingKey("rc.user");

        action.setDestination(amqpMessageDestination);

        businessActionRepository.save(action);
    }

    private void seedUnixAccountDeleteActions() {
        BusinessAction action;
        AmqpMessageDestination amqpMessageDestination;

        //UnixAccount delete
        action = new BusinessAction();
        action.setBusinessActionType(BusinessActionType.UNIX_ACCOUNT_DELETE_RC);
        action.setName("UnixAccount delete");

        amqpMessageDestination = new AmqpMessageDestination();
        amqpMessageDestination.setExchange("unix-account.delete");
        amqpMessageDestination.setRoutingKey("rc.user");

        action.setDestination(amqpMessageDestination);

        action.setPriority(1);

        businessActionRepository.save(action);
    }

    private void seedSeoOrderActions() {
        BusinessAction action;
        MailManagerMessageDestination mailManagerMessageDestination;

        //Seo order
        action = new BusinessAction();
        action.setBusinessActionType(BusinessActionType.SEO_ORDER_MM);
        action.setName("Seo order MM");

        mailManagerMessageDestination = new MailManagerMessageDestination();

        action.setDestination(mailManagerMessageDestination);

        action.setPriority(1);

        businessActionRepository.save(action);
    }
}
