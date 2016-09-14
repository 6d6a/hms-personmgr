package ru.majordomo.hms.personmgr.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

import ru.majordomo.hms.personmgr.common.ActionType;
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
        this.seedDatabaseCreateActions();
        this.seedDatabaseUpdateActions();
        this.seedMailboxCreateActions();
        this.seedMailboxUpdateActions();

        logger.info("BusinessAction found with findAll():");
        logger.info("-------------------------------");

        List<BusinessAction> businessActions = businessActionRepository.findAll();
        if (businessActions.size() > 0) {
            result = true;
        }
        for (BusinessAction businessAction : businessActions) {
            logger.info("businessAction: " + businessAction.toString());
        }

        return result;
    }

    private void seedWebSiteCreateActions() {
        BusinessAction action;
        AmqpMessageDestination amqpMessageDestination;
        MailManagerMessageDestination mailManagerMessageDestination;

        //WebSite create
        action = new BusinessAction();
        action.setActionType(ActionType.WEB_SITE_CREATE_RC);
        action.setName("WebSite create RC");

        amqpMessageDestination = new AmqpMessageDestination();
        amqpMessageDestination.setExchange("website.create");
        amqpMessageDestination.setRoutingKey("service.rc.user");

        action.setDestination(amqpMessageDestination);

        action.setPriority(1);

        businessActionRepository.save(action);

        action = new BusinessAction();
        action.setActionType(ActionType.WEB_SITE_CREATE_MM);
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
        action.setActionType(ActionType.WEB_SITE_UPDATE_RC);
        action.setName("WebSite update");

        amqpMessageDestination = new AmqpMessageDestination();
        amqpMessageDestination.setExchange("website.update");
        amqpMessageDestination.setRoutingKey("service.rc.user");

        action.setDestination(amqpMessageDestination);

        action.setPriority(1);

        businessActionRepository.save(action);
    }

    private void seedDatabaseCreateActions() {
        BusinessAction action;
        AmqpMessageDestination amqpMessageDestination;


        //Database create
        action = new BusinessAction();
        action.setActionType(ActionType.DATABASE_CREATE_RC);
        action.setName("Database create");

        amqpMessageDestination = new AmqpMessageDestination();
        amqpMessageDestination.setExchange("database.create");
        amqpMessageDestination.setRoutingKey("service.rc.user");

        action.setDestination(amqpMessageDestination);

        businessActionRepository.save(action);
    }

    private void seedDatabaseUpdateActions() {
        BusinessAction action;
        AmqpMessageDestination amqpMessageDestination;

        //Database update
        action = new BusinessAction();
        action.setActionType(ActionType.DATABASE_UPDATE_RC);
        action.setName("Database update");

        amqpMessageDestination = new AmqpMessageDestination();
        amqpMessageDestination.setExchange("database.update");
        amqpMessageDestination.setRoutingKey("service.rc.user");

        action.setDestination(amqpMessageDestination);

        businessActionRepository.save(action);
    }

    private void seedMailboxCreateActions() {
        BusinessAction action;
        AmqpMessageDestination amqpMessageDestination;


        //Database create
        action = new BusinessAction();
        action.setActionType(ActionType.MAILBOX_CREATE_RC);
        action.setName("Mailbox create");

        amqpMessageDestination = new AmqpMessageDestination();
        amqpMessageDestination.setExchange("mailbox.create");
        amqpMessageDestination.setRoutingKey("service.rc.user");

        action.setDestination(amqpMessageDestination);

        businessActionRepository.save(action);
    }

    private void seedMailboxUpdateActions() {
        BusinessAction action;
        AmqpMessageDestination amqpMessageDestination;

        //Database update
        action = new BusinessAction();
        action.setActionType(ActionType.MAILBOX_UPDATE_RC);
        action.setName("Mailbox update");

        amqpMessageDestination = new AmqpMessageDestination();
        amqpMessageDestination.setExchange("mailbox.update");
        amqpMessageDestination.setRoutingKey("service.rc.user");

        action.setDestination(amqpMessageDestination);

        businessActionRepository.save(action);
    }
}
