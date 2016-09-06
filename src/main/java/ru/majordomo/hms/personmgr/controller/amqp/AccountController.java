//package ru.majordomo.hms.personmgr.controller.amqp;
//
//import org.springframework.amqp.rabbit.annotation.EnableRabbit;
//import org.springframework.amqp.rabbit.annotation.Exchange;
//import org.springframework.amqp.rabbit.annotation.Queue;
//import org.springframework.amqp.rabbit.annotation.QueueBinding;
//import org.springframework.amqp.rabbit.annotation.RabbitListener;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.messaging.handler.annotation.Headers;
//import org.springframework.messaging.handler.annotation.Payload;
//import org.springframework.stereotype.Service;
//
//import java.util.HashMap;
//import java.util.Map;
//
//import ru.majordomo.hms.personmgr.common.MailManagerTask;
//import ru.majordomo.hms.personmgr.common.message.amqp.CreateModifyMessage;
//import ru.majordomo.hms.personmgr.common.operation.Operation;
//import ru.majordomo.hms.personmgr.service.AmqpSender;
//import ru.majordomo.hms.personmgr.service.MailManager;
//
//@EnableRabbit
//@Service
//public class AccountController {
//
//    @Autowired
//    private AmqpSender amqpSender;
//
//    @Autowired
//    private MailManager mailManager;
//
//    @Autowired
//    private Operation operation;
//
//    private final Map<Object, Object> EMPTY_PARAMS = new HashMap<>();
//
//    private final String EXCHANGE_PREFIX = "account.";
//
//    @RabbitListener(bindings = @QueueBinding(value = @Queue(durable = "true", autoDelete = "true"), exchange = @Exchange(value = EXCHANGE_PREFIX + "create"), key = "pm"))
//    public void createAction(@Payload CreateModifyMessage message, @Headers Map<String, String> headers) {
////        String provider = headers.get("provider");
////        System.out.println("Received from " + provider + ": " + message.toString());
////        operation.setOperationIdentity(message.getOperationIdentity());
////        if (!message.getObjRef().isEmpty()) {
////            System.out.println("RedisOperation -> success " + provider + ", operationIdentity: " + message.getOperationIdentity());
////            operation.successOperation(provider);
////            operation.setParams(provider, message.getParams());
////            if (provider.equals("rc")) {
////                message.setParams(EMPTY_PARAMS);
////                amqpSender.send("website.create", "fin", message);
////                System.out.println("Sent to FIN: " + message.toString());
////            }
////        } else {
////            System.out.println("RedisOperation -> error " + provider + ", operationIdentity: " + message.getOperationIdentity());
////            operation.errorOperation(provider, "Ошибка");
////        }
////        if (operation.isSuccess()) {
////            MailManagerTask mailTask = new MailManagerTask();
////            mailTask.setApiName("MajordomoVHClientCreatedConfirmation");
////            mailTask.setEmail(operation.getParams("request").get("email").toString());
////            mailTask.addParameter("client_id", "12345");
////            mailTask.addParameter("password", "*****");
////            mailTask.addParameter("ftp_ip", "1.2.3.4");
////            mailTask.addParameter("ftp_login", "12345");
////            mailTask.addParameter("ftp_password", "*****");
////            mailTask.setPriority(10);
////
////            mailManager.createTask(mailTask);
////        }
//    }
////
////    @RabbitListener(bindings = @QueueBinding(value = @Queue(durable = "true", autoDelete = "true"), exchange = @Exchange(value = "account.modify"), key = "pm"))
////    public void modifyAccountAction(@Payload CreateModifyMessage message, @Headers Map<String, String> headers) {
////        System.out.println("Recieved message:");
////        System.out.println(message.toString());
////        String provider = headers.get("provider");
////        System.out.println(provider);
////    }
//}
