package ru.majordomo.hms.personmgr.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ru.majordomo.hms.personmgr.common.message.GenericMessageDestination;
import ru.majordomo.hms.personmgr.common.message.ServiceMessage;
import ru.majordomo.hms.personmgr.common.message.amqp.AmqpMessageDestination;
import ru.majordomo.hms.personmgr.common.message.amqp.CreateModifyMessage;
import ru.majordomo.hms.personmgr.model.ProcessingBusinessAction;

/**
 * BusinessFlowActionProcessor
 */
@Service
public class BusinessFlowActionProcessor {
    private final static Logger logger = LoggerFactory.getLogger(BusinessFlowActionProcessor.class);

    @Autowired
    private AmqpSender amqpSender;

    public ProcessingBusinessAction process(ProcessingBusinessAction action) {
        logger.info("processing BusinessAction " + action.getId());

        ServiceMessage message = new ServiceMessage();
        message.setOperationIdentity(action.getBusinessFlowId());
        message.setActionIdentity(action.getId());
        message.setParams(action.getParams());

        GenericMessageDestination destination = action.getDestination();
        switch (destination.getType()) {
            case AMQP:
                AmqpMessageDestination amqpMessageDestination = (AmqpMessageDestination) action.getDestination();
                amqpSender.send(amqpMessageDestination.getExchange(), amqpMessageDestination.getRoutingKey(), message);
        }

        return action;
    }

}
