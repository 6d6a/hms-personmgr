package ru.majordomo.hms.personmgr.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

import ru.majordomo.hms.personmgr.common.ActionType;
import ru.majordomo.hms.personmgr.common.message.MailManagerMessage;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.common.message.destination.MailManagerMessageDestination;
import ru.majordomo.hms.personmgr.common.message.MailManagerMessageParams;
import ru.majordomo.hms.personmgr.common.message.WebSiteCreateMessage;
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

//        SimpleServiceMessage message = new WebSiteCreateMessage();
//
//        action.setMessage(message);

        action.setPriority(1);

        businessActionRepository.save(action);

        action = new BusinessAction();
        action.setActionType(ActionType.WEB_SITE_CREATE_MM);
        action.setName("WebSite create MM");

        mailManagerMessageDestination = new MailManagerMessageDestination();

        action.setDestination(mailManagerMessageDestination);

//        SimpleServiceMessage mailManagerMessage = new MailManagerMessage();
//        MailManagerMessageParams mailManagerMessageParams = new MailManagerMessageParams();
//        mailManagerMessageParams.setApi_name("MajordomoVHWebSiteCreated");
//        mailManagerMessageParams.setPriority(10);
//
//        mailManagerMessage.setParams(mailManagerMessageParams);
//
//        action.setMessage(mailManagerMessage);

        action.setPriority(2);

        businessActionRepository.save(action);
    }

//    private void seedWebSiteUpdateActions() {
//        BusinessAction action;
//        AmqpMessageDestination amqpMessageDestination;
//        MailManagerMessageDestination mailManagerMessageDestination;
//
//        //WebSite create
//        action = new BusinessAction();
//        action.setActionType(ActionType.WEB_SITE_CREATE_RC);
//        action.setName("WebSite create");

//        amqpMessageDestination = new AmqpMessageDestination();
//        amqpMessageDestination.setExchange("website.create");
//        amqpMessageDestination.setRoutingKey("service.rc.user");
//
//        action.setDestination(amqpMessageDestination);
//
//        WebSiteCreateMessage message = new WebSiteCreateMessage();
//
//        action.setMessage(message);
//
//        action.setPriority(1);
//
//        businessActionRepository.save(action);
//
//        action = new BusinessAction();
//
//        mailManagerMessageDestination = new MailManagerMessageDestination();
//
//        action.setDestination(mailManagerMessageDestination);
//
//        MailManagerMessage mailManagerMessage = new MailManagerMessage();
//        MailManagerMessageParams mailManagerMessageParams = new MailManagerMessageParams();
//        mailManagerMessageParams.setApi_name("MajordomoVHWebSiteCreated");
////        mailManagerMessageParams.setEmail(mailManagerDevEmail);
//        mailManagerMessageParams.setPriority(10);
//
////        HashMap<String, String> parameters = new HashMap<>();
////        parameters.put("client_id", "12345");
////        parameters.put("website_name", "test-site.ru");
////        mailManagerMessageParams.setParametrs(parameters);
//
//        mailManagerMessage.setParams(mailManagerMessageParams);
//        action.setMessage(mailManagerMessage);
//
//        action.setPriority(2);
//
//        businessActionRepository.save(action);
//    }

    private void seedDatabaseCreateActions() {
//        BusinessAction action;
//        AmqpMessageDestination amqpMessageDestination;
//        MailManagerMessageDestination mailManagerMessageDestination;

        //Database create
//        action = new BusinessAction();
//        action.setActionType(ActionType.DATABASE_CREATE);
//        action.setName("Database create");
//
//        destination = new AmqpMessageDestination();
//        destination.setExchange("database.create");
//        destination.setRoutingKey("service.rc.user");
//
//        action.setDestination(destination);
//
//        WebSiteCreateMessage message = new WebSiteCreateMessage();
//
//        action.setMessage(message);
//
//        action.setMessage("");
//
//        businessActionRepository.save(action);
    }
}
