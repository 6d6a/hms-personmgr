package ru.majordomo.hms.personmgr.service;

import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import ru.majordomo.hms.personmgr.models.message.Message;
import ru.majordomo.hms.personmgr.models.message.ReportMessage;
import ru.majordomo.hms.personmgr.service.RabbitMqConfig;

import java.util.List;

@EnableRabbit
@Service
public class AmqpConsumer
{

    @RabbitListener(queues = {"hms.report"})
    public void onMessage(@Payload ReportMessage message) {
        System.out.println("Recieved message:");
        System.out.println(message.toString());
        if (message.getResult()) {
            System.out.println("Делаю вид, что сообщаю об успехе");
        } else {
            System.out.println("Делаю вид, что сообщаю об ошибке");
        }
    }
}
