package ru.majordomo.hms.personmgr.controller.amqp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

import ru.majordomo.hms.personmgr.common.BusinessActionType;
import ru.majordomo.hms.personmgr.common.BusinessOperationType;
import ru.majordomo.hms.personmgr.common.State;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.model.PersonalAccount;
import ru.majordomo.hms.personmgr.model.ProcessingBusinessOperation;
import ru.majordomo.hms.personmgr.repository.PersonalAccountRepository;
import ru.majordomo.hms.personmgr.repository.ProcessingBusinessOperationRepository;
import ru.majordomo.hms.personmgr.service.BusinessActionBuilder;
import ru.majordomo.hms.personmgr.service.BusinessFlowDirector;
import ru.majordomo.hms.personmgr.service.PromocodeProcessor;

@EnableRabbit
@Service
public class AmqpPersonController {

    private final static Logger logger = LoggerFactory.getLogger(AmqpPersonController.class);

    private final BusinessFlowDirector businessFlowDirector;
    private final PersonalAccountRepository accountRepository;
    private final ProcessingBusinessOperationRepository processingBusinessOperationRepository;
    private final BusinessActionBuilder businessActionBuilder;

    @Autowired
    public AmqpPersonController(
            BusinessFlowDirector businessFlowDirector,
            PersonalAccountRepository accountRepository,
            ProcessingBusinessOperationRepository processingBusinessOperationRepository,
            BusinessActionBuilder businessActionBuilder
    ) {
        this.businessFlowDirector = businessFlowDirector;
        this.accountRepository = accountRepository;
        this.processingBusinessOperationRepository = processingBusinessOperationRepository;
        this.businessActionBuilder = businessActionBuilder;
    }

    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(
                            value = "pm.person.create",
                            durable = "true",
                            autoDelete = "true"
                    ),
                    exchange = @Exchange(
                            value = "person.create",
                            type = ExchangeTypes.TOPIC
                    ),
                    key = "pm"
            )
    )
    public void create(@Payload SimpleServiceMessage message, @Headers Map<String, String> headers) {
        String provider = headers.get("provider");
        logger.debug("Received from " + provider + ": " + message.toString());

        PersonalAccount account = accountRepository.findOne(message.getAccountId());

        if (account != null && account.getOwnerPersonId() == null) {
            String objRef = message.getObjRef();
            String personId = objRef.substring(objRef.lastIndexOf("/") + 1);
            if (objRef.equals(personId)) {
                logger.error("Не удалось получить personId из objRef: " + objRef);
            } else {
                account.setOwnerPersonId(personId);
                accountRepository.save(account);
            }
        }

        State state = businessFlowDirector.processMessage(message);

        if (state == State.PROCESSED) {
            ProcessingBusinessOperation businessOperation = processingBusinessOperationRepository.findOne(message.getOperationIdentity());
            if (businessOperation != null && businessOperation.getType() == BusinessOperationType.ACCOUNT_CREATE) {
                message.addParam("quota", businessOperation.getMapParams().get("quota"));
                businessActionBuilder.build(BusinessActionType.UNIX_ACCOUNT_CREATE_RC, message);
            }
        }
    }

    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(
                            value = "pm.person.update",
                            durable = "true",
                            autoDelete = "true"
                    ),
                    exchange = @Exchange(
                            value = "person.update",
                            type = ExchangeTypes.TOPIC
                    ),
                    key = "pm"
            )
    )
    public void update(@Payload SimpleServiceMessage message, @Headers Map<String, String> headers) {
        String provider = headers.get("provider");
        logger.debug("Received update message from " + provider + ": " + message.toString());

        State state = businessFlowDirector.processMessage(message);
    }

    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(
                            value = "pm.person.delete",
                            durable = "true",
                            autoDelete = "true"
                    ),
                    exchange = @Exchange(
                            value = "person.delete",
                            type = ExchangeTypes.TOPIC
                    ),
                    key = "pm"
            )
    )
    public void delete(@Payload SimpleServiceMessage message, @Headers Map<String, String> headers) {
        String provider = headers.get("provider");
        logger.debug("Received delete message from " + provider + ": " + message.toString());

        State state = businessFlowDirector.processMessage(message);
    }
}
