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
import ru.majordomo.hms.personmgr.common.message.ResponseMessage;
import ru.majordomo.hms.personmgr.service.BusinessFlowDirector;

@EnableRabbit
@Service
public class AmqpAccountController {

    private final static Logger logger = LoggerFactory.getLogger(AmqpAccountController.class);

    @Autowired
    private BusinessFlowDirector businessFlowDirector;

    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = "pm.account.create", durable = "true", autoDelete = "true"), exchange = @Exchange(value = "account.create", type = ExchangeTypes.TOPIC), key = "pm"))
    public void create(@Payload ResponseMessage message, @Headers Map<String, String> headers) {
        String provider = headers.get("provider");
        logger.info("Received from " + provider + ": " + message.toString());

        businessFlowDirector.processMessage(message);
    }

    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = "pm.account.update", durable = "true", autoDelete = "true"), exchange = @Exchange(value = "account.update", type = ExchangeTypes.TOPIC), key = "pm"))
    public void update(@Payload ResponseMessage message, @Headers Map<String, String> headers) {
        String provider = headers.get("provider");
        logger.info("Received update message from " + provider + ": " + message.toString());

        State state = businessFlowDirector.processMessage(message);
    }

    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = "pm.account.delete", durable = "true", autoDelete = "true"), exchange = @Exchange(value = "account.delete", type = ExchangeTypes.TOPIC), key = "pm"))
    public void delete(@Payload ResponseMessage message, @Headers Map<String, String> headers) {
        String provider = headers.get("provider");
        logger.info("Received delete message from " + provider + ": " + message.toString());

        State state = businessFlowDirector.processMessage(message);
    }
}
