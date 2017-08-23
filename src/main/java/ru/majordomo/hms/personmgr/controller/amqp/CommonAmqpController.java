package ru.majordomo.hms.personmgr.controller.amqp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;

import java.net.MalformedURLException;
import java.net.URL;

import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.repository.ProcessingBusinessActionRepository;
import ru.majordomo.hms.personmgr.service.BusinessFlowDirector;

public class CommonAmqpController {
    protected Logger logger = LoggerFactory.getLogger(getClass());

    protected BusinessFlowDirector businessFlowDirector;
    protected ProcessingBusinessActionRepository processingBusinessActionRepository;
    protected PersonalAccountManager accountManager;
    protected ApplicationEventPublisher publisher;

    @Autowired
    public void setBusinessFlowDirector(BusinessFlowDirector businessFlowDirector) {
        this.businessFlowDirector = businessFlowDirector;
    }

    @Autowired
    public void setProcessingBusinessActionRepository(
            ProcessingBusinessActionRepository processingBusinessActionRepository
    ) {
        this.processingBusinessActionRepository = processingBusinessActionRepository;
    }

    @Autowired
    public void setAccountManager(PersonalAccountManager accountManager) {
        this.accountManager = accountManager;
    }

    @Autowired
    public void setPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    protected String getResourceIdByObjRef(String url) {
        try {
            URL processingUrl = new URL(url);
            String path = processingUrl.getPath();
            String[] pathParts = path.split("/");

            return pathParts[2];
        } catch (MalformedURLException e) {
            e.printStackTrace();
            logger.error("Got Exception in ru.majordomo.hms.personmgr.controller.amqp.CommonAmqpController.getResourceIdByObjRef " + e.getMessage());
            return null;
        }
    }
}
