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

import ru.majordomo.hms.personmgr.common.BusinessActionType;
import ru.majordomo.hms.personmgr.common.State;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.event.account.AccountDomainAutoRenewCompletedEvent;
import ru.majordomo.hms.personmgr.model.PersonalAccount;
import ru.majordomo.hms.personmgr.model.ProcessingBusinessAction;
import ru.majordomo.hms.personmgr.repository.PersonalAccountRepository;
import ru.majordomo.hms.personmgr.repository.ProcessingBusinessActionRepository;
import ru.majordomo.hms.personmgr.service.BusinessFlowDirector;

import static ru.majordomo.hms.personmgr.common.Constants.AUTO_RENEW_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.RESOURCE_ID_KEY;

@EnableRabbit
@Service
public class DomainAmqpController {

    private final static Logger logger = LoggerFactory.getLogger(DomainAmqpController.class);

    private final BusinessFlowDirector businessFlowDirector;
    private final ProcessingBusinessActionRepository processingBusinessActionRepository;
    private final PersonalAccountRepository personalAccountRepository;
    private final ApplicationEventPublisher publisher;

    @Autowired
    public DomainAmqpController(
            BusinessFlowDirector businessFlowDirector,
            ProcessingBusinessActionRepository processingBusinessActionRepository,
            PersonalAccountRepository personalAccountRepository,
            ApplicationEventPublisher publisher) {
        this.businessFlowDirector = businessFlowDirector;
        this.processingBusinessActionRepository = processingBusinessActionRepository;
        this.personalAccountRepository = personalAccountRepository;
        this.publisher = publisher;
    }

    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(
                            value = "pm.domain.create",
                            durable = "true",
                            autoDelete = "false"
                    ),
                    exchange = @Exchange(
                            value = "domain.create",
                            type = ExchangeTypes.TOPIC
                    ),
                    key = "pm"
            )
    )
    public void create(@Payload SimpleServiceMessage message, @Headers Map<String, String> headers) {
        String provider = headers.get("provider");
        logger.debug("Received from " + provider + ": " + message.toString());

        State state = businessFlowDirector.processMessage(message);
    }

    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(
                            value = "pm.domain.update",
                            durable = "true",
                            autoDelete = "false"
                    ),
                    exchange = @Exchange(
                            value = "domain.update",
                            type = ExchangeTypes.TOPIC
                    ),
                    key = "pm"
            )
    )
    public void update(@Payload SimpleServiceMessage message, @Headers Map<String, String> headers) {
        String provider = headers.get("provider");
        logger.debug("Received update message from " + provider + ": " + message.toString());

        State state = businessFlowDirector.processMessage(message);

        if (state == State.PROCESSED) {
            ProcessingBusinessAction businessAction = processingBusinessActionRepository.findOne(message.getActionIdentity());

            if (businessAction != null
                    && businessAction.getBusinessActionType().equals(BusinessActionType.DOMAIN_UPDATE_RC)
                    && (Boolean) businessAction.getParam(AUTO_RENEW_KEY)) {
                PersonalAccount account = personalAccountRepository.findOne(businessAction.getPersonalAccountId());

                Map<String, String> params = new HashMap<>();
                params.put(RESOURCE_ID_KEY, (String) businessAction.getParam(RESOURCE_ID_KEY));

                publisher.publishEvent(new AccountDomainAutoRenewCompletedEvent(account, params));
            }
        }
    }

    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(
                            value = "pm.domain.delete",
                            durable = "true",
                            autoDelete = "false"
                    ),
                    exchange = @Exchange(
                            value = "domain.delete",
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
