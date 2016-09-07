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

import java.util.HashMap;
import java.util.Map;

import ru.majordomo.hms.personmgr.common.MailManagerTask;
import ru.majordomo.hms.personmgr.common.State;
import ru.majordomo.hms.personmgr.common.message.ServiceMessage;
import ru.majordomo.hms.personmgr.model.ProcessingBusinessFlow;
import ru.majordomo.hms.personmgr.repository.ProcessingBusinessFlowRepository;
import ru.majordomo.hms.personmgr.service.AmqpSender;
import ru.majordomo.hms.personmgr.service.MailManager;

@EnableRabbit
@Service
public class AmqpDatabaseController {

    @Autowired
    private AmqpSender amqpSender;

    @Autowired
    private MailManager mailManager;

    @Autowired
    private ProcessingBusinessFlowRepository businessFlowRepository;

    private final static Logger logger = LoggerFactory.getLogger(AmqpDatabaseController.class);

    private final Map<Object, Object> EMPTY_PARAMS = new HashMap<>();

    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = "service.pm.database", durable = "true", autoDelete = "true"), exchange = @Exchange(value = "database.create", type = ExchangeTypes.TOPIC), key = "service.pm"))
    public void create(@Payload ServiceMessage message, @Headers Map<String, String> headers) {
        String provider = headers.get("provider");
        logger.info("Received from " + provider + ": " + message.toString());

        ProcessingBusinessFlow businessFlow = businessFlowRepository.findOne(message.getOperationIdentity());
        if (message.containsParam("success") && message.getParam("success").equals(true) && businessFlow != null) {
            logger.info("ProcessingBusinessFlow -> success " + provider + ", operationIdentity: " + message.getOperationIdentity());
            businessFlow.setState(State.PROCESSED);
//            operation.successOperation(provider);
//            operation.setParams(provider, message.getParams());
//            if (provider.equals("rc")) {
//                message.setParams(EMPTY_PARAMS);
//                amqpSender.send("website.create", "fin", message);
//                System.out.println("Sent to FIN: " + message.toString());
//            }
        } else {
            logger.info("ProcessingBusinessFlow -> error " + provider + ", operationIdentity: " + message.getOperationIdentity());
            if (businessFlow != null) {
                businessFlow.setState(State.ERROR);
            }
        }
        if (businessFlow != null && businessFlow.getState() == State.PROCESSED) {
            MailManagerTask mailTask = new MailManagerTask();
            mailTask.setApiName("MajordomoVHWebSiteCreated");
            mailTask.setEmail("web-script@majordomo.ru");
            mailTask.addParameter("client_id", "12345");
            mailTask.addParameter("website_name", "b1234556");
            mailTask.setPriority(10);

            mailManager.createTask(mailTask);

            logger.info("mail sent");
        }
    }
}
