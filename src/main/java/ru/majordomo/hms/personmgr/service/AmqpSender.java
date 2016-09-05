package ru.majordomo.hms.personmgr.service;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ru.majordomo.hms.personmgr.common.message.GenericMessage;

@Service
public class AmqpSender {

    @Autowired
    private RabbitTemplate myRabbitTemplate;

    private Message createMessage(GenericMessage genericMessage, MessageProperties messageProperties) {

        return MessageBuilder
                .withBody(genericMessage.toJson().getBytes())
                .andProperties(messageProperties)
                .build();
    }

    public void send(String exchange, String routingKey, GenericMessage message) {
        myRabbitTemplate.setExchange(exchange);
        MessageProperties messageProperties = new MessageProperties();
        messageProperties.setHeader("provider", "pm");
        Message amqpMessage = createMessage(message, messageProperties);
        myRabbitTemplate.convertAndSend(routingKey, amqpMessage);
    }
}
