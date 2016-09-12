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

import ru.majordomo.hms.personmgr.common.message.ResponseMessage;
import ru.majordomo.hms.personmgr.repository.ProcessingBusinessActionRepository;
import ru.majordomo.hms.personmgr.service.AmqpSender;
import ru.majordomo.hms.personmgr.service.BusinessFlowDirector;

@EnableRabbit
@Service
public class AmqpDatabaseController {

    private final static Logger logger = LoggerFactory.getLogger(AmqpDatabaseController.class);
    @Autowired
    private AmqpSender amqpSender;
//    @Autowired
//    private MailManager mailManager;
    @Autowired
    private ProcessingBusinessActionRepository businessFlowRepository;

    @Autowired
    private BusinessFlowDirector businessFlowDirector;

    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = "service.pm.database", durable = "true", autoDelete = "true"), exchange = @Exchange(value = "database.create", type = ExchangeTypes.TOPIC), key = "service.pm"))
    public void create(@Payload ResponseMessage message, @Headers Map<String, String> headers) {
        String provider = headers.get("provider");
        logger.info("Received from " + provider + ": " + message.toString());

        businessFlowDirector.processMessage(message);
    }
}
