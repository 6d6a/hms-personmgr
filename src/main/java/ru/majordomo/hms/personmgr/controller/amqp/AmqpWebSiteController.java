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
import ru.majordomo.hms.personmgr.common.message.ResponseMessage;
import ru.majordomo.hms.personmgr.model.ProcessingBusinessFlow;
import ru.majordomo.hms.personmgr.repository.ProcessingBusinessFlowRepository;
import ru.majordomo.hms.personmgr.service.AmqpSender;
import ru.majordomo.hms.personmgr.service.MailManager;

@EnableRabbit
@Service
public class AmqpWebSiteController {

    private final static Logger logger = LoggerFactory.getLogger(AmqpWebSiteController.class);
    private final Map<Object, Object> EMPTY_PARAMS = new HashMap<>();
    @Autowired
    private AmqpSender amqpSender;
    @Autowired
    private MailManager mailManager;
    @Autowired
    private ProcessingBusinessFlowRepository businessFlowRepository;

    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = "service.pm.website.create", durable = "true", autoDelete = "true"), exchange = @Exchange(value = "website.create", type = ExchangeTypes.TOPIC), key = "service.pm"))
    public void create(@Payload ResponseMessage message, @Headers Map<String, String> headers) {
        String provider = headers.get("provider");
        logger.info("Received from " + provider + ": " + message.toString());

        ProcessingBusinessFlow businessFlow = businessFlowRepository.findOne(message.getOperationIdentity());
        if (message.getParams().isSuccess() && businessFlow != null) {
            logger.info("ProcessingBusinessFlow -> success " + provider + ", operationIdentity: " + message.getOperationIdentity());
            businessFlow.setState(State.PROCESSED);
            businessFlow.setProcessBusinessActionStateById(message.getActionIdentity(), State.PROCESSED);
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
                businessFlow.setProcessBusinessActionStateById(message.getActionIdentity(), State.ERROR);
            }
        }
        if (businessFlow != null && businessFlow.getState() == State.PROCESSED) {
            MailManagerTask mailTask = new MailManagerTask();
            mailTask.setApiName("MajordomoVHWebSiteCreated");
            mailTask.setEmail("web-script@majordomo.ru");
            mailTask.addParameter("client_id", "12345");
            mailTask.addParameter("website_name", "test-site.ru");
            mailTask.setPriority(10);

            mailManager.createTask(mailTask);

            logger.info("mail sent");
        }

        if (businessFlow != null) {
            businessFlowRepository.save(businessFlow);
        }
    }

    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = "service.pm.website.create", durable = "true", autoDelete = "true"), exchange = @Exchange(value = "website.modify", type = ExchangeTypes.TOPIC), key = "service.pm"))
    public void modify(@Payload ResponseMessage message, @Headers Map<String, String> headers) {
        String provider = headers.get("provider");
        logger.info("Received from " + provider + ": " + message.toString());

        ProcessingBusinessFlow businessFlow = businessFlowRepository.findOne(message.getOperationIdentity());
        if (message.getParams().isSuccess() && businessFlow != null) {
            logger.info("ProcessingBusinessFlow -> success " + provider + ", operationIdentity: " + message.getOperationIdentity());
            businessFlow.setState(State.PROCESSED);
            businessFlow.setProcessBusinessActionStateById(message.getActionIdentity(), State.PROCESSED);
        } else {
            logger.info("ProcessingBusinessFlow -> error " + provider + ", operationIdentity: " + message.getOperationIdentity());
            if (businessFlow != null) {
                businessFlow.setState(State.ERROR);
                businessFlow.setProcessBusinessActionStateById(message.getActionIdentity(), State.ERROR);
            }
        }

        if (businessFlow != null) {
            businessFlowRepository.save(businessFlow);
        }
    }
}
