package ru.majordomo.hms.personmgr.restcontrollers.accountcontroller;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import ru.majordomo.hms.personmgr.service.AmqpController;
import ru.majordomo.hms.personmgr.models.operation.Operation;
import ru.majordomo.hms.personmgr.models.message.amqp.CreateModifyMessage;
import ru.majordomo.hms.personmgr.models.message.rest.external.RestMessage;
import ru.majordomo.hms.personmgr.models.message.rest.resourcecontroller.servers.ActiveHostingServerMessage;
import ru.majordomo.hms.personmgr.models.message.rest.resourcecontroller.servers.ActiveMailStorageServerMessage;
import ru.majordomo.hms.personmgr.models.Response;
import ru.majordomo.hms.personmgr.restcontrollers.RestHelper;

import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.HashMap;

@RestController
public class AccountRestController {

    @Autowired
    private Operation operation;

    @Autowired
    private AmqpController amqpController;

    @Autowired
    Logger logger;

    @RequestMapping(value = "/pm/account/new", method = RequestMethod.POST)
    public Response createAccount(
            @RequestBody String requestBody,
            HttpServletResponse response
    ) {
        //handling request - getting body params
        RestMessage restMessage = RestHelper.getFromJson(requestBody);

        String operationIdentity = restMessage.getOperationIdentity();

        HashMap<Object, Object> data = restMessage.getData();

        if (!RestHelper.isValidOperationIdentity(operationIdentity)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return new Response("0", "Bad operationIdentity");
        }

        //check all necessary params in body
        if (!RestHelper.hasRequiredParams(restMessage, "account.create")) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return new Response(operationIdentity, "Bad request");
        }

        RestTemplate restTemplate = new RestTemplate();
        ActiveHostingServerMessage activeHostingServerMessage = restTemplate.getForObject("http://127.0.0.1:5000/rc/hostingServer/active", ActiveHostingServerMessage.class);
        String activeHostingServer = activeHostingServerMessage.getHostingServer().getHostingServerId();
        ActiveMailStorageServerMessage activeMailStorageServerMessage = restTemplate.getForObject("http://127.0.0.1:5000/rc/mailStorageServer/active", ActiveMailStorageServerMessage.class);
        String activeMailStorageServer = activeMailStorageServerMessage.getMailStorageServer().getMailStorageServerId();

        //set services for feedback waiting
        ArrayList<String> requiredFeedback = new ArrayList<>();
        requiredFeedback.add("rc");
        requiredFeedback.add("fin");

        operation.setOperationIdentity(operationIdentity);
        operation.newOperation(requiredFeedback);
        operation.setParams("request", data);
        logger.info(restMessage.toString());

        CreateModifyMessage createModifyMessage = new CreateModifyMessage();

        createModifyMessage.setOperationIdentity(operationIdentity);
        data.put("hostServerId", activeHostingServer);
        data.put("mailStorageServerId", activeMailStorageServer);
        createModifyMessage.setParams(data);
        amqpController.send("account.create", "rc", createModifyMessage);

        response.setStatus(HttpServletResponse.SC_ACCEPTED);
        return new Response(operationIdentity, "queued");
    }
}
