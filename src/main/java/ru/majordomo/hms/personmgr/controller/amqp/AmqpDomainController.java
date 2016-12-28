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

import java.util.Map;

import ru.majordomo.hms.personmgr.common.State;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.service.BusinessFlowDirector;
import ru.majordomo.hms.personmgr.service.FinFeignClient;

@EnableRabbit
@Service
public class AmqpDomainController {

    private final static Logger logger = LoggerFactory.getLogger(AmqpDomainController.class);

    private final BusinessFlowDirector businessFlowDirector;
    private final FinFeignClient finFeignClient;

    @Autowired
    public AmqpDomainController(BusinessFlowDirector businessFlowDirector, FinFeignClient finFeignClient) {
        this.businessFlowDirector = businessFlowDirector;
        this.finFeignClient = finFeignClient;
    }

    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = "pm.domain.create", durable = "true", autoDelete = "true"), exchange = @Exchange(value = "domain.create", type = ExchangeTypes.TOPIC), key = "pm"))
    public void create(@Payload SimpleServiceMessage message, @Headers Map<String, String> headers) {
        String provider = headers.get("provider");
        logger.debug("Received from " + provider + ": " + message.toString());

        State state = businessFlowDirector.processMessage(message);

        processBlockedPayment(state, message);
    }

    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = "pm.domain.update", durable = "true", autoDelete = "true"), exchange = @Exchange(value = "domain.update", type = ExchangeTypes.TOPIC), key = "pm"))
    public void update(@Payload SimpleServiceMessage message, @Headers Map<String, String> headers) {
        String provider = headers.get("provider");
        logger.debug("Received update message from " + provider + ": " + message.toString());

        State state = businessFlowDirector.processMessage(message);

        processBlockedPayment(state, message);
    }

    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = "pm.domain.delete", durable = "true", autoDelete = "true"), exchange = @Exchange(value = "domain.delete", type = ExchangeTypes.TOPIC), key = "pm"))
    public void delete(@Payload SimpleServiceMessage message, @Headers Map<String, String> headers) {
        String provider = headers.get("provider");
        logger.debug("Received delete message from " + provider + ": " + message.toString());

        State state = businessFlowDirector.processMessage(message);
    }

    private void processBlockedPayment(State state, SimpleServiceMessage message) {
        if (state == State.PROCESSED && message.getParam("documentNumber") != null) {
            finFeignClient.chargeBlocked(message.getAccountId(), (String) message.getParam("documentNumber"));
            //Спишем заблокированные средства
        } else if (state == State.ERROR && message.getParam("documentNumber") != null) {
            //Разблокируем средства
            finFeignClient.unblock(message.getAccountId(), (String) message.getParam("documentNumber"));
        }
    }
}
