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
import ru.majordomo.hms.personmgr.model.ProcessingBusinessFlow;
import ru.majordomo.hms.personmgr.repository.ProcessingBusinessActionRepository;
import ru.majordomo.hms.personmgr.service.AmqpSender;

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

    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = "service.pm.database", durable = "true", autoDelete = "true"), exchange = @Exchange(value = "database.create", type = ExchangeTypes.TOPIC), key = "service.pm"))
    public void create(@Payload ResponseMessage message, @Headers Map<String, String> headers) {
        String provider = headers.get("provider");
        logger.info("Received from " + provider + ": " + message.toString());

        ProcessingBusinessFlow businessFlow = businessFlowRepository.findOne(message.getOperationIdentity());
        if (message.getParams().isSuccess() && businessFlow != null) {
            logger.info("ProcessingBusinessFlow -> success " + provider + ", operationIdentity: " + message.getOperationIdentity());
            businessFlow.setState(State.PROCESSED);
        } else {
            logger.info("ProcessingBusinessFlow -> error " + provider + ", operationIdentity: " + message.getOperationIdentity());
            if (businessFlow != null) {
                businessFlow.setState(State.ERROR);
            }
        }
        if (businessFlow != null) {
            businessFlowRepository.save(businessFlow);
        }
    }
}
