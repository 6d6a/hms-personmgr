package ru.majordomo.hms.personmgr.controller;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import ru.majordomo.hms.personmgr.models.message.Message;
import ru.majordomo.hms.personmgr.service.AmqpSender;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@RestController
public class Rest {

    private static final String freeTemplate = "created %s as a free service!";
    private static final String paidTemplate = "created %s as a paid service!";

    private final AtomicLong counter = new AtomicLong();

    @Autowired
    private RabbitTemplate myRabbitTemplate;

    @RequestMapping("/account/{accountId}/{accountType}/{action}")
    public Response response(
            @RequestParam (value = "name", defaultValue = "ac_111") String name,
            @RequestParam (value = "operationIdentity", defaultValue = "0") String operationIdentity,
            @PathVariable (value = "accountId") String accountId,
            @PathVariable (value = "accountType") String accountType,
            @PathVariable (value = "action") String action
            )
    {
        AmqpSender amqpSender = new AmqpSender(myRabbitTemplate);
        Message message = new Message();
        if (operationIdentity.equals("0")) {
            return new Response("0", "Bad operationIdentity");
        }

        int ftpReal = 2;
        int ftpAllow = 3;

//        RestTemplate restTemplate = new RestTemplate();
//        String rcRestResponse = restTemplate.getForObject("http://apigw.intr/api/v1/rc/account/"+accountId+"/"+accountType+"/count", String.class);
//        String finRestResponse = restTemplate.getForObject("http://apigw.intr/api/v1/fin/account/"+accountId+"/plan/"+accountType+"/count", String.class);

        String rcRestResponse = "{\"ftpCount\":\"" + ftpReal + "\"}";
        System.out.println(rcRestResponse);
        String finRestResponse = "{\"ftpCount\":\"" + ftpAllow + "\"}";
        System.out.println(finRestResponse);
        Boolean free = ftpReal < ftpAllow;

        message.setOperationIdentity(operationIdentity);
        List<String> accId = new ArrayList<>();
        accId.add(accountId);
        message.setAccountIdentity(accId);
        Map<String, String> operation = new HashMap<>();
        operation.put("action", action);
        operation.put("accountType", accountType);

        if (free) {
            operation.put("free", "false");
        } else {
            operation.put("free", "true");
        }
        message.setOperation(operation);
        amqpSender.sendMessage("hms.event.new", message);
        if (free) {
            return new Response(operationIdentity,
                    String.format(freeTemplate, name));
        } else {
            return new Response(operationIdentity,
                    String.format(paidTemplate, name));
        }
    }

}
