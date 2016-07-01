package ru.majordomo.hms.personmgr.service;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import ru.majordomo.hms.personmgr.models.message.Message;

@EnableRabbit
@Service
public class AmqpSender {

    private RabbitTemplate myRabbitTemplate;

    @Autowired
    public AmqpSender(RabbitTemplate myRabbitTemplate) {
        this.myRabbitTemplate = myRabbitTemplate;
    }
    public void sendMessage(String queueName, Message message)
    {
        myRabbitTemplate.convertAndSend(queueName, message.toJson());
    }
}
