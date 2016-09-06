package ru.majordomo.hms.personmgr.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ru.majordomo.hms.personmgr.common.message.GenericMessage;
import ru.majordomo.hms.personmgr.common.message.ServiceMessage;

@Service
public class AmqpSender {

    @Autowired
    private RabbitTemplate myRabbitTemplate;

    private final static Logger logger = LoggerFactory.getLogger(AmqpSender.class);

    private Message createMessage(ServiceMessage genericMessage, MessageProperties messageProperties) {

        return MessageBuilder
                .withBody(genericMessage.toJson().getBytes())
                .andProperties(messageProperties)
                .build();
    }

    public void send(String exchange, String routingKey, ServiceMessage message) {
        logger.info("send message by AmqpSender - exchange: " + exchange +" routingKey: " + routingKey + " message " + message.toString());

        myRabbitTemplate.setExchange(exchange);
        MessageProperties messageProperties = new MessageProperties();
        messageProperties.setHeader("provider", "pm");
        messageProperties.setContentType("application/json");
        Message amqpMessage = createMessage(message, messageProperties);
        logger.info(amqpMessage.toString());
        myRabbitTemplate.convertAndSend(routingKey, amqpMessage);
    }
}
