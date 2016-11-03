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

import ru.majordomo.hms.personmgr.common.message.ImportMessage;
import ru.majordomo.hms.personmgr.service.importing.PlanDBImportService;

@EnableRabbit
@Service
public class AmqpPlanController {

    private final static Logger logger = LoggerFactory.getLogger(AmqpPlanController.class);

    @Autowired
    private PlanDBImportService planDBImportService;

    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = "pm.plan.import", durable = "true", autoDelete = "true"), exchange = @Exchange(value = "plan.import", type = ExchangeTypes.TOPIC), key = "pm"))
    public void importPlan(@Payload ImportMessage message, @Headers Map<String, String> headers) {
        String provider = headers.get("provider");
        logger.info("Received from " + provider + ": " + message.toString());

        planDBImportService.processImportMessage(message);
    }
}
