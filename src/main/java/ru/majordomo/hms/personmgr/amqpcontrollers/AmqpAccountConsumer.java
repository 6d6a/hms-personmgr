package ru.majordomo.hms.personmgr.amqpcontrollers;

import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import ru.majordomo.hms.personmgr.models.operation.Operation;
import ru.majordomo.hms.personmgr.models.mailmanager.NewMailTask;
import ru.majordomo.hms.personmgr.models.message.amqp.CreateModifyMessage;
import ru.majordomo.hms.personmgr.service.AmqpController;
import ru.majordomo.hms.personmgr.service.MailManager;

import java.util.HashMap;
import java.util.Map;

@EnableRabbit
@Service
public class AmqpAccountConsumer
{

    @Autowired
    private AmqpController amqpController;

    @Autowired
    private Operation operation;

    private final Map<Object, Object> EMPTY_PARAMS = new HashMap<>();

    private final String EXCHANGE = "account.create";

    @RabbitListener(bindings = @QueueBinding(value = @Queue(durable = "true", autoDelete = "true"), exchange = @Exchange(value = EXCHANGE), key = "pm"))
    public void createAccountAction(@Payload CreateModifyMessage message, @Headers Map<String, String> headers) {
        String provider = headers.get("provider");
        System.out.println("Recieved from " + provider + ": " + message.toString());
        operation.setOperationIdentity(message.getOperationIdentity());
        if (!message.getObjRef().isEmpty()) {
            System.out.println("RedisOperation -> success " + provider + ", operationIdentity: " + message.getOperationIdentity());
            operation.successOperation(provider);
            operation.setParams(provider, message.getParams());
            if (provider.equals("rc")) {
                message.setParams(EMPTY_PARAMS);
                amqpController.send("account.create", "fin", message);
                System.out.println("Sent to FIN: " + message.toString());
            }
        } else {
            System.out.println("RedisOperation -> error " + provider + ", operationIdentity: " + message.getOperationIdentity());
            operation.errorOperation(provider, "Ошибка");
        }
        if (operation.isSuccess()) {
            NewMailTask mailTask = new NewMailTask();
            mailTask.setApi_name("mj_scheduled_works_18_03_2016").setEmail(operation.getParams("request").get("email").toString()).addParametr("client_id", "12345").setPriority(10);
//            System.out.println(operation.getParams("request").get("email").toString());

            MailManager mailManager = new MailManager();
            mailManager.createTask(mailTask);
        }
    }

    @RabbitListener(bindings = @QueueBinding(value = @Queue(durable = "true", autoDelete = "true"), exchange = @Exchange(value = "account.modify"), key = "pm"))
    public void modifyAccountAction(@Payload CreateModifyMessage message, @Headers Map<String, String> headers) {
        System.out.println("Recieved message:");
        System.out.println(message.toString());
        String provider = headers.get("provider");
        System.out.println(provider);
    }
}
