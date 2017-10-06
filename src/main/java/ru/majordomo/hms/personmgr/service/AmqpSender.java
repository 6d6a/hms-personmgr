package ru.majordomo.hms.personmgr.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;

@Service
public class AmqpSender {
    private final static Logger logger = LoggerFactory.getLogger(AmqpSender.class);
    private final RabbitTemplate myRabbitTemplate;
    private String instanceName;
    private String fullApplicationName;

    @Autowired
    public AmqpSender(
            RabbitTemplate myRabbitTemplate,
            @Value("${spring.application.name}") String applicationName,
            @Value("${hms.instance.name}") String instanceName
    ) {
        this.myRabbitTemplate = myRabbitTemplate;
        this.instanceName = instanceName;
        this.fullApplicationName = instanceName + "." + applicationName;
    }

    private Message createMessage(
            SimpleServiceMessage message,
            MessageProperties messageProperties
    ) {
        return MessageBuilder
                .withBody(message.toJson().getBytes())
                .andProperties(messageProperties)
                .build();
    }

    public void send(String exchange, String routingKey, SimpleServiceMessage message) {
        if (!routingKey.startsWith(instanceName + ".")) {
            routingKey = instanceName + "." + routingKey;
        }

        logger.debug("send message by AmqpSender - exchange: " + exchange +
                " routingKey: " + routingKey + " message " + message.toString());

        MessageProperties messageProperties = new MessageProperties();
        messageProperties.setHeader("provider", fullApplicationName);
        messageProperties.setContentType("application/json");

        Message amqpMessage = createMessage(message, messageProperties);

        logger.debug(amqpMessage.toString());

        myRabbitTemplate.send(exchange, routingKey, amqpMessage);

        logger.info("ACTION_IDENTITY: " + message.getActionIdentity() +
                " OPERATION_IDENTITY: " + message.getOperationIdentity() +
                " Сообщение от: " + fullApplicationName + " " +
                "в exchange: " + exchange + " " +
                "с routing key: " + routingKey + " " +
                "отправлено." + " " +
                "Вот оно: " + message.toString());
    }
}
