package ru.majordomo.hms.personmgr.service;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import ru.majordomo.hms.personmgr.models.OperationState;
import ru.majordomo.hms.personmgr.models.message.ReportMessage;

@EnableRabbit
@Service
public class AmqpConsumer
{

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @RabbitListener(queues = {"hms.report"})
    public void onMessage(@Payload ReportMessage message) {
        System.out.println("Recieved message:");
        System.out.println(message.toString());
        if (message.getResult()) {
            System.out.println("RedisOperation -> success");
            OperationState operationState = new OperationState(redisTemplate, message.getOperationIdentity());
            operationState.successOperation();
        } else {
            System.out.println("RedisOperation -> error");
            OperationState operationState = new OperationState(redisTemplate, message.getOperationIdentity());
            operationState.errorOperation();
        }
    }
}
