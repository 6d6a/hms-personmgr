package ru.majordomo.hms.personmgr.service;

import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;

import ru.majordomo.hms.personmgr.common.FlowType;
import ru.majordomo.hms.personmgr.common.message.MailManagerMessage;
import ru.majordomo.hms.personmgr.common.message.MailManagerMessageDestination;
import ru.majordomo.hms.personmgr.common.message.MailManagerMessageParams;
import ru.majordomo.hms.personmgr.common.message.WebSiteCreateMessage;
import ru.majordomo.hms.personmgr.common.message.AmqpMessageDestination;
import ru.majordomo.hms.personmgr.model.BusinessAction;
import ru.majordomo.hms.personmgr.model.BusinessFlow;
import ru.majordomo.hms.personmgr.repository.BusinessActionRepository;
import ru.majordomo.hms.personmgr.repository.BusinessFlowRepository;

/**
 * Сервис для загрузки первичных данных в БД
 */
@Service
public class BusinessFlowDBSeedService {
    private final static Logger logger = LoggerFactory.getLogger(BusinessFlowDBSeedService.class);

    @Autowired
    private BusinessFlowRepository businessFlowRepository;

    @Autowired
    private BusinessActionRepository businessActionRepository;

    @Value( "${mail_manager.dev_email}" )
    private String mailManagerDevEmail;

    private ObjectMapper mapper = new ObjectMapper();

    public boolean seedDB() {
        boolean result = false;

        businessFlowRepository.deleteAll();
        businessActionRepository.deleteAll();

        this.seedWebSiteCreateFlow();

        logger.info("BusinessFlow found with findAll():");
        logger.info("-------------------------------");

        List<BusinessFlow> businessFlows = businessFlowRepository.findAll();
        if (businessFlows.size() > 0) {
            result = true;
        }
        for (BusinessFlow businessFlow : businessFlows) {
            logger.info("businessFlow: " + businessFlow.toString());
        }

        return result;
    }

    private void seedWebSiteCreateFlow() {
        BusinessFlow flow;
        BusinessAction action;
        AmqpMessageDestination amqpMessageDestination;
        MailManagerMessageDestination mailManagerMessageDestination;

        //WebSite create
        flow = new BusinessFlow();
        flow.setFlowType(FlowType.WEB_SITE_CREATE);
        flow.setName("WebSite create");

        businessFlowRepository.save(flow);

        action = new BusinessAction();

        amqpMessageDestination = new AmqpMessageDestination();
        amqpMessageDestination.setExchange("website.create");
        amqpMessageDestination.setRoutingKey("service.rc.user");

        action.setDestination(amqpMessageDestination);

        WebSiteCreateMessage message = new WebSiteCreateMessage();

        action.setMessage(message);

        action.setBusinessFlowId(flow.getId());
        action.setPriority(1);

        businessActionRepository.save(action);

        action = new BusinessAction();

        mailManagerMessageDestination = new MailManagerMessageDestination();

        action.setDestination(mailManagerMessageDestination);

        MailManagerMessage mailManagerMessage = new MailManagerMessage();
        MailManagerMessageParams mailManagerMessageParams = new MailManagerMessageParams();
        mailManagerMessageParams.setApiName("MajordomoVHWebSiteCreated");
//        mailManagerMessageParams.setEmail(mailManagerDevEmail);
        mailManagerMessageParams.setPriority(10);

//        HashMap<String, String> parameters = new HashMap<>();
//        parameters.put("client_id", "12345");
//        parameters.put("website_name", "test-site.ru");
//        mailManagerMessageParams.setParameters(parameters);

        mailManagerMessage.setParams(mailManagerMessageParams);
        action.setMessage(mailManagerMessage);

        action.setBusinessFlowId(flow.getId());
        action.setPriority(2);

        businessActionRepository.save(action);
    }

//    private void seedWebSiteUpdateFlow() {
//        BusinessFlow flow;
//        BusinessAction action;
//        AmqpMessageDestination amqpMessageDestination;
//        MailManagerMessageDestination mailManagerMessageDestination;
//
//        businessFlowRepository.deleteAll();
//        businessActionRepository.deleteAll();
//
//        //WebSite create
//        flow = new BusinessFlow();
//        flow.setFlowType(FlowType.WEB_SITE_CREATE);
//        flow.setName("WebSite create");
//
//        businessFlowRepository.save(flow);
//
//        action = new BusinessAction();
//
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
//        action.setBusinessFlowId(flow.getId());
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
//        mailManagerMessageParams.setApiName("MajordomoVHWebSiteCreated");
////        mailManagerMessageParams.setEmail(mailManagerDevEmail);
//        mailManagerMessageParams.setPriority(10);
//
////        HashMap<String, String> parameters = new HashMap<>();
////        parameters.put("client_id", "12345");
////        parameters.put("website_name", "test-site.ru");
////        mailManagerMessageParams.setParameters(parameters);
//
//        mailManagerMessage.setParams(mailManagerMessageParams);
//        action.setMessage(mailManagerMessage);
//
//        action.setBusinessFlowId(flow.getId());
//        action.setPriority(2);
//
//        businessActionRepository.save(action);
//    }

    private void seedDatabaseCreateFlow() {
        BusinessFlow flow;
        BusinessAction action;
        AmqpMessageDestination amqpMessageDestination;
        MailManagerMessageDestination mailManagerMessageDestination;

        //Database create
//        flow = new BusinessFlow();
//        flow.setFlowType(FlowType.DATABASE_CREATE);
//        flow.setName("Database create");
//
//        businessFlowRepository.save(flow);
//
//        action = new BusinessAction();
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
//        action.setBusinessFlowId(flow.getId());
//
//        businessActionRepository.save(action);
    }
}
