package ru.majordomo.hms.personmgr.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ru.majordomo.hms.personmgr.common.message.destination.GenericMessageDestination;
import ru.majordomo.hms.personmgr.common.message.destination.AmqpMessageDestination;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessAction;

@Service
public class BusinessActionProcessor {
    private final static Logger logger = LoggerFactory.getLogger(BusinessActionProcessor.class);

    private final AmqpSender amqpSender;

    @Autowired
    public BusinessActionProcessor(AmqpSender amqpSender) {
        this.amqpSender = amqpSender;
    }

    public ProcessingBusinessAction process(ProcessingBusinessAction action) {
        logger.debug("processing BusinessAction " + action.getId());

        action.getMessage().setOperationIdentity(action.getOperationId());
        action.getMessage().setActionIdentity(action.getId());

        GenericMessageDestination destination = action.getDestination();
        logger.debug("BusinessAction destination type: " + destination.getType());
        switch (destination.getType()) {
            case AMQP:
                AmqpMessageDestination amqpMessageDestination = (AmqpMessageDestination) action.getDestination();
                amqpSender.send(amqpMessageDestination.getExchange(), amqpMessageDestination.getRoutingKey(), action.getMessage());

                break;
            case MAIL_MANAGER:
                logger.error("MAIL_MANAGER destinationType not used any more (deprecated)");

                break;
        }

        return action;
    }

}
