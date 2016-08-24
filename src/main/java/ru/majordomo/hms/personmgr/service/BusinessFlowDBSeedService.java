package ru.majordomo.hms.personmgr.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

import ru.majordomo.hms.personmgr.common.FlowType;
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

    public boolean seedDB() {
        boolean result = false;

        businessFlowRepository.deleteAll();
        businessActionRepository.deleteAll();


        BusinessFlow webSiteCreate = new BusinessFlow();
        webSiteCreate.setFlowType(FlowType.WEB_SITE_CREATE);
        webSiteCreate.setName("WebSite create");

        businessFlowRepository.save(webSiteCreate);

        BusinessAction webSiteCreateAction = new BusinessAction();
        webSiteCreateAction.setDestination("rc");
        webSiteCreateAction.setMessage("Create this f***ing WebSite");
        webSiteCreateAction.setBusinessFlowId(webSiteCreate.getId());

        businessActionRepository.save(webSiteCreateAction);

                // fetch all customers
        logger.info("BusinessFlow found with findAll():");
        logger.info("-------------------------------");

        List<BusinessFlow> businessFlows = businessFlowRepository.findAll();
        if(businessFlows.size() > 0) {
            result = true;
        }
        for (BusinessFlow businessFlow : businessFlows) {
            logger.info("businessFlow: " + businessFlow.toString());
        }

        return result;
    }
}
