package ru.majordomo.hms.personmgr.service;

import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

import ru.majordomo.hms.personmgr.common.FlowType;
import ru.majordomo.hms.personmgr.common.message.WebSiteCreateMessage;
import ru.majordomo.hms.personmgr.common.message.amqp.AmqpMessageDestination;
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

    private ObjectMapper mapper = new ObjectMapper();

    public boolean seedDB() {
        boolean result = false;
        BusinessFlow flow;
        BusinessAction action;
        AmqpMessageDestination destination;

        businessFlowRepository.deleteAll();
        businessActionRepository.deleteAll();

        //WebSite create
        flow = new BusinessFlow();
        flow.setFlowType(FlowType.WEB_SITE_CREATE);
        flow.setName("WebSite create");

        businessFlowRepository.save(flow);

        action = new BusinessAction();

        destination = new AmqpMessageDestination();
        destination.setExchange("website.create");
        destination.setRoutingKey("service.rc.user");

        action.setDestination(destination);

        WebSiteCreateMessage message = new WebSiteCreateMessage();

        action.setMessage(message);

        action.setBusinessFlowId(flow.getId());

        businessActionRepository.save(action);

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

        // fetch all customers
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
}
