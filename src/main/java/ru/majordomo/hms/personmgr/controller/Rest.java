package ru.majordomo.hms.personmgr.controller;

import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import ru.majordomo.hms.personmgr.models.message.Message;
import ru.majordomo.hms.personmgr.models.OperationState;
import ru.majordomo.hms.personmgr.models.message.RestMessage;
import ru.majordomo.hms.personmgr.service.AmqpSender;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class Rest {

    private static final String freeCreatedTemplate = "created FTP account for %s as a free service!";
    private static final String paidCreatedTemplate = "created FTP account for %s as a paid service!";
    private static final String modifiedTemplate = "modified FTP account for %s as a free service!";

    @Autowired
    private RabbitTemplate myRabbitTemplate;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private RestMessage getFromJson(String jsonString) {
        ObjectMapper mapper = new ObjectMapper();
        RestMessage restMessage = new RestMessage();
        try {
            restMessage = mapper.readValue(jsonString, RestMessage.class);
        } catch (IOException e) {
            e.getMessage();
        }
        if (restMessage.getOperationIdentity() == null) {
            restMessage.setOperationIdentity("0");
        }
        return restMessage;
    }

    private Boolean isValidOperationIdentity(String operationIdentity) {
        //TODO do some real validation
        return (!operationIdentity.equals("0"));
    }

    private HashMap<String, String> convertJsonToHashMap(String jsonString) {
        ObjectMapper mapper = new ObjectMapper();
        HashMap<String, String> newObject = new HashMap<>();
        try {
            newObject = mapper.readValue(jsonString, HashMap.class);
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        return newObject;
    }

    /**
     * creates ftpAccount for accountObject{accountID}
     * @param accountId - main account object ID
     * @param requestBody - json encoded request body
     * @param response - HttpServlet
     * @return API Response instance
     */
    @RequestMapping(value = "/pm/account/{accountId}/ftpAccount/new", method = RequestMethod.POST)
    public Response createFtpAccount(
            @PathVariable (value = "accountId") String accountId,
            @RequestBody String requestBody,
            HttpServletResponse response
            )
    {
        //handling request - getting body params
        RestMessage restMessage = getFromJson(requestBody);
        System.out.println("Recieved request: " + restMessage.toString());

        String operationIdentity = restMessage.getOperationIdentity();

        //checking for correct operationIdentity
        if (!isValidOperationIdentity(operationIdentity)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return new Response("0", "Bad operationIdentity");
        }

//        RestTemplate restTemplate = new RestTemplate();
//        String rcRestResponse = restTemplate.getForObject("http://apigw.intr/api/v1/rc/account/"+accountId+"/"+accountType+"/count", String.class);
//        String finRestResponse = restTemplate.getForObject("http://apigw.intr/api/v1/fin/account/"+accountId+"/plan/"+accountType+"/count", String.class);

        //testing values
        int ftpReal = 2;
        int ftpAllow = 3;
        //make a decision: free or paid account
        Boolean free = ftpReal < ftpAllow;

        //create new event and send it to event.BUS
        AmqpSender amqpSender = new AmqpSender(myRabbitTemplate);

        List<String> accId = new ArrayList<>();
        accId.add(accountId);

        Message message = new Message(operationIdentity, accId);
        message.setOperation("create", "ftpAccount", free.toString());
        message.setData(restMessage.getData());

        amqpSender.sendMessage("hms.event.new", message);

        //persisting operation in Redis
        OperationState operationState = new OperationState(redisTemplate, operationIdentity);
        operationState.newOperation();

        //giving HTTP response
        response.setStatus(HttpServletResponse.SC_ACCEPTED);
        if (free) {
            return new Response(operationIdentity,
                    String.format(freeCreatedTemplate, accountId));
        } else {
            return new Response(operationIdentity,
                    String.format(paidCreatedTemplate, accountId));
        }
    }

    /**
     * creates ftpAccount for accountObject{accountID}
     * @param accountId - main account object ID
     * @param requestBody - json encoded request body
     * @param response - HttpServlet
     * @return API Response instance
     */
    @RequestMapping(value = "/pm/account/{accountId}/ftpAccount", method = RequestMethod.POST)
    public Response modifyFtpAccount(
            @PathVariable (value = "accountId") String accountId,
            @RequestBody String requestBody,
            HttpServletResponse response
    )
    {
        //handling request - getting body params
        RestMessage restMessage = getFromJson(requestBody);
        System.out.println("Recieved request: " + restMessage.toString());

        String operationIdentity = restMessage.getOperationIdentity();

        //checking for correct operationIdentity
        if (!isValidOperationIdentity(operationIdentity)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return new Response("0", "Bad operationIdentity");
        }

        RestTemplate restTemplate = new RestTemplate();
//        String rcRestResponse = restTemplate.getForObject("http://apigw.intr/api/v1/rc/account/"+accountId+"/ftpAccount", String.class);

        //testing response from RC
//        String rcRestResponse = "{\"operationIdentity\":\"12345\",\"ftpAccounts\"[{\"id\":\"15\",\"name\":\"ololo\",\"password\":\"qwerty\"}]}";
        String rcRestResponse = restTemplate.getForObject("http://127.0.0.1:5000/rc/account/"+accountId+"/ftpAccounts", String.class);
        System.out.println(rcRestResponse);
        HashMap<String, String> rcRestResponseObject = convertJsonToHashMap(rcRestResponse);
        System.out.println("Serialized RC response: " + rcRestResponseObject.toString());

        HashMap<String, String> data = restMessage.getData();
//        if (!data.get("name"))) {
//            return new Response("0", "No such account");
//        }
//
//        if (rcRestResponseObject.get(data.get("name")).equals(data.get("password"))) {
//            return new Response("0", "Not changed");
//        }

        //create new event and send it to event.BUS
        AmqpSender amqpSender = new AmqpSender(myRabbitTemplate);

        List<String> accId = new ArrayList<>();
        accId.add(accountId);

        Message message = new Message(operationIdentity, accId);
        message.setOperation("modify", "ftpAccount", "true");
        message.setData(restMessage.getData());

        amqpSender.sendMessage("hms.event.new", message);

        //persisting operation in Redis
        OperationState operationState = new OperationState(redisTemplate, operationIdentity);
        operationState.newOperation();

        //giving HTTP response
        response.setStatus(HttpServletResponse.SC_ACCEPTED);
        return new Response(operationIdentity,
                String.format(modifiedTemplate, accountId));
    }

    /**
     * Fetches operation state from Redis
     * @param operationIdentity - operation ID
     * @param response - HTTP Servlet
     * @return API Response
     */
    @RequestMapping(value = "/pm/account/checkOperation", method = RequestMethod.GET)
    public Response response(@RequestParam(value = "operationIdentity", defaultValue = "0") String operationIdentity,
                             HttpServletResponse response) {
        //checking for correct operationIdentity
        if (isValidOperationIdentity(operationIdentity)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return new Response("0", "Bad operationIdentity");
        } else {
            OperationState operationState = new OperationState(redisTemplate, operationIdentity);

            //TODO try-catch
            String currentState = operationState.fetchOperationState();

            response.setStatus(HttpServletResponse.SC_OK);
            return new Response(operationIdentity, currentState);
        }
    }
}
