package ru.majordomo.hms.personmgr.controller.amqp;

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
import ru.majordomo.hms.personmgr.common.message.amqp.CreateModifyMessage;
import ru.majordomo.hms.personmgr.model.ProcessingBusinessFlow;
import ru.majordomo.hms.personmgr.repository.ProcessingBusinessFlowRepository;
import ru.majordomo.hms.personmgr.service.AmqpSender;
import ru.majordomo.hms.personmgr.service.MailManager;

@EnableRabbit
@Service
public class AmqpWebSiteController {

    @Autowired
    private AmqpSender amqpSender;

    @Autowired
    private MailManager mailManager;

    @Autowired
    private ProcessingBusinessFlowRepository businessFlowRepository;

    private final Map<Object, Object> EMPTY_PARAMS = new HashMap<>();

    private final String EXCHANGE_PREFIX = "website.";

    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = "service.pm", durable = "true", autoDelete = "true"), exchange = @Exchange(value = EXCHANGE_PREFIX + "create"), key = "pm"))
    public void createAction(@Payload CreateModifyMessage message, @Headers Map<String, String> headers) {
        String provider = headers.get("provider");
        System.out.println("Received from " + provider + ": " + message.toString());

        ProcessingBusinessFlow businessFlow = businessFlowRepository.findOne(message.getOperationIdentity());
        if (!message.getObjRef().isEmpty()) {
            System.out.println("ProcessingBusinessFlow -> success " + provider + ", operationIdentity: " + message.getOperationIdentity());
            businessFlow.setState(State.PROCESSED);
//            operation.successOperation(provider);
//            operation.setParams(provider, message.getParams());
//            if (provider.equals("rc")) {
//                message.setParams(EMPTY_PARAMS);
//                amqpSender.send("website.create", "fin", message);
//                System.out.println("Sent to FIN: " + message.toString());
//            }
        } else {
            System.out.println("ProcessingBusinessFlow -> error " + provider + ", operationIdentity: " + message.getOperationIdentity());
            businessFlow.setState(State.ERROR);
        }
        if (businessFlow.getState() == State.PROCESSED) {
            MailManagerTask mailTask = new MailManagerTask();
            mailTask.setApiName("MajordomoVHWebSiteCreated");
            mailTask.setEmail("web-script@majordomo.ru");
            mailTask.addParameter("client_id", "12345");
            mailTask.addParameter("website_name", "test-site.ru");
            mailTask.setPriority(10);

            mailManager.createTask(mailTask);
        }
    }
//
//    @RabbitListener(bindings = @QueueBinding(value = @Queue(durable = "true", autoDelete = "true"), exchange = @Exchange(value = "account.modify"), key = "pm"))
//    public void modifyAccountAction(@Payload CreateModifyMessage message, @Headers Map<String, String> headers) {
//        System.out.println("Recieved message:");
//        System.out.println(message.toString());
//        String provider = headers.get("provider");
//        System.out.println(provider);
//    }
}
