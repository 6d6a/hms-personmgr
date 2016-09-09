package ru.majordomo.hms.personmgr.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ru.majordomo.hms.personmgr.common.MailManagerTask;
import ru.majordomo.hms.personmgr.common.message.MailManagerMessage;
import ru.majordomo.hms.personmgr.common.message.ServiceMessage;
import ru.majordomo.hms.personmgr.common.message.GenericMessageDestination;
import ru.majordomo.hms.personmgr.common.message.AmqpMessageDestination;
import ru.majordomo.hms.personmgr.model.ProcessingBusinessAction;

/**
 * BusinessFlowActionProcessor
 */
@Service
public class BusinessFlowActionProcessor {
    private final static Logger logger = LoggerFactory.getLogger(BusinessFlowActionProcessor.class);

    @Autowired
    private AmqpSender amqpSender;

    @Autowired
    private MailManager mailManager;

    public ProcessingBusinessAction process(ProcessingBusinessAction action) {
        logger.info("processing BusinessAction " + action.getId());

        ServiceMessage message = action.getMessage();
        message.setOperationIdentity(action.getBusinessFlowId());
        message.setActionIdentity(action.getId());
//        message.setParams(action.getParams());

        GenericMessageDestination destination = action.getDestination();
        switch (destination.getType()) {
            case AMQP:
                AmqpMessageDestination amqpMessageDestination = (AmqpMessageDestination) action.getDestination();
                amqpSender.send(amqpMessageDestination.getExchange(), amqpMessageDestination.getRoutingKey(), message);

                break;
            case MAIL_MANAGER:
//                MailManagerMessageDestination mailManagerMessageDestination = (MailManagerMessageDestination) action.getDestination();
//                amqpSender.send(MailManagerMessageDestination.getExchange(), MailManagerMessageDestination.getRoutingKey(), message);
//                MailManagerTask mailTask = new MailManagerTask();
//                mailTask.setApiName("MajordomoVHWebSiteCreated");
//                mailTask.setEmail("web-script@majordomo.ru");
//                mailTask.addParameter("client_id", "12345");
//                mailTask.addParameter("website_name", "test-site.ru");
//                mailTask.setPriority(10);

                mailManager.send((MailManagerMessage)action.getMessage());

                logger.info("mail sent");

                break;
        }

        return action;
    }

}
