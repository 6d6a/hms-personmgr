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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

import ru.majordomo.hms.personmgr.common.BusinessOperationType;
import ru.majordomo.hms.personmgr.common.State;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.event.account.AccountCreatedEvent;
import ru.majordomo.hms.personmgr.model.PersonalAccount;
import ru.majordomo.hms.personmgr.model.ProcessingBusinessOperation;
import ru.majordomo.hms.personmgr.repository.PersonalAccountRepository;
import ru.majordomo.hms.personmgr.repository.ProcessingBusinessOperationRepository;
import ru.majordomo.hms.personmgr.service.BusinessFlowDirector;

import static ru.majordomo.hms.personmgr.common.Constants.PASSWORD_KEY;

@EnableRabbit
@Service
public class UnixAccountAmqpController {

    private final static Logger logger = LoggerFactory.getLogger(UnixAccountAmqpController.class);

    private final BusinessFlowDirector businessFlowDirector;
    private final ProcessingBusinessOperationRepository processingBusinessOperationRepository;
    private final PersonalAccountRepository personalAccountRepository;
    private final ApplicationEventPublisher publisher;

    @Autowired
    public UnixAccountAmqpController(
            BusinessFlowDirector businessFlowDirector,
            ProcessingBusinessOperationRepository processingBusinessOperationRepository,
            PersonalAccountRepository personalAccountRepository,
            ApplicationEventPublisher publisher) {
        this.businessFlowDirector = businessFlowDirector;
        this.processingBusinessOperationRepository = processingBusinessOperationRepository;
        this.personalAccountRepository = personalAccountRepository;
        this.publisher = publisher;
    }

    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(
                            value = "pm.unix-account.create",
                            durable = "true",
                            autoDelete = "false"
                    ),
                    exchange = @Exchange(
                            value = "unix-account.create",
                            type = ExchangeTypes.TOPIC
                    ),
                    key = "pm"
            )
    )

    public void create(@Payload SimpleServiceMessage message, @Headers Map<String, String> headers) {
        String provider = headers.get("provider");
        logger.debug("Received from " + provider + ": " + message.toString());

        State state = businessFlowDirector.processMessage(message);

        if (state == State.PROCESSED) {
            ProcessingBusinessOperation businessOperation = processingBusinessOperationRepository.findOne(message.getOperationIdentity());
            if (businessOperation != null && businessOperation.getType() == BusinessOperationType.ACCOUNT_CREATE) {
                businessOperation.setState(State.PROCESSED);
                processingBusinessOperationRepository.save(businessOperation);

                PersonalAccount account = personalAccountRepository.findOne(businessOperation.getPersonalAccountId());
                Map<String, String> params = new HashMap<>();
                params.put(PASSWORD_KEY, (String) businessOperation.getParam(PASSWORD_KEY));

                publisher.publishEvent(new AccountCreatedEvent(account, params));
            }
        }
    }

    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(
                            value = "pm.unix-account.update",
                            durable = "true",
                            autoDelete = "false"
                    ),
                    exchange = @Exchange(
                            value = "unix-account.update",
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
                            value = "pm.unix-account.delete",
                            durable = "true",
                            autoDelete = "false"
                    ),
                    exchange = @Exchange(
                            value = "unix-account.delete",
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
