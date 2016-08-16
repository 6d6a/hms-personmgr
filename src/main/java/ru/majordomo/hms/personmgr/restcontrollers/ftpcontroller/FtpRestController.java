package ru.majordomo.hms.personmgr.restcontrollers.ftpcontroller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import ru.majordomo.hms.personmgr.service.AmqpController;
import ru.majordomo.hms.personmgr.models.operation.Operation;
import ru.majordomo.hms.personmgr.models.message.amqp.CreateModifyMessage;
import ru.majordomo.hms.personmgr.models.message.rest.external.RestMessage;
import ru.majordomo.hms.personmgr.models.message.rest.resourcecontroller.ftp.FtpAccountCountMessage;
import ru.majordomo.hms.personmgr.models.message.rest.resourcecontroller.ftp.FtpAccountMessage;
import ru.majordomo.hms.personmgr.models.Response;
import ru.majordomo.hms.personmgr.restcontrollers.RestHelper;

import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.HashMap;

import static java.lang.Integer.parseInt;

@RestController
public class FtpRestController {

    private static final String freeCreatedTemplate = "created FTP account for %s as a free service!";
    private static final String paidCreatedTemplate = "created FTP account for %s as a paid service!";
    private static final String modifiedTemplate = "modified FTP account for %s as a free service!";

    @Autowired
    private Operation operation;

    @Autowired
    private AmqpController amqpController;

    /**
     * creates ftpAccount for accountObject{accountID}
     * @param accountId - main account object ID
     * @param requestBody - json encoded request body
     * @param response - HttpServlet
     * @return API Response instance
     */
    @RequestMapping(value = "/pm/account/{accountId}/ftpAccount/new", method = RequestMethod.POST)
    public Response createFtpAccount(
            @PathVariable(value = "accountId") String accountId,
            @RequestBody String requestBody,
            HttpServletResponse response
            )
    {
        //handling request - getting body params
        RestMessage restMessage = RestHelper.getFromJson(requestBody);
        System.out.println(restMessage.toString());

        String operationIdentity = restMessage.getOperationIdentity();

        //checking for correct operationIdentity
        if (!RestHelper.isValidOperationIdentity(operationIdentity)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return new Response("0", "Bad operationIdentity");
        }

        //check all necessary params in body
        if (!RestHelper.hasRequiredParams(restMessage, "ftp.create")) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return new Response(operationIdentity, "Bad request");
        }

//        RestTemplate restTemplate = new RestTemplate();
//        //TODO make real requests
//        FtpAccountCountMessage rcRestResponse = restTemplate.getForObject("http://127.0.0.1:5000/rc/account/"+accountId+"/ftpAccount/count", FtpAccountCountMessage.class);
//        FtpAccountCountMessage finRestResponse = restTemplate.getForObject("http://127.0.0.1:5000/fin/account/"+accountId+"/plan/ftpAccount/count", FtpAccountCountMessage.class);
//        Integer ftpReal = rcRestResponse.getCount();
//        Integer ftpAllow = finRestResponse.getCount();

        //make a decision: free or paid account
//        Boolean free = ftpReal < ftpAllow;

        //set services for feedback waiting
        ArrayList<String> requiredFeedback = new ArrayList<>();
        requiredFeedback.add("rc");
        requiredFeedback.add("fin");
        requiredFeedback.add("te");

        operation.setOperationIdentity(operationIdentity);
        operation.newOperation(requiredFeedback);

        CreateModifyMessage createModifyMessage = new CreateModifyMessage();
        createModifyMessage.setOperationIdentity(operationIdentity);
        createModifyMessage.setParams(restMessage.getData());

        amqpController.send("ftp.create", "rc", createModifyMessage);

        //giving HTTP response
        response.setStatus(HttpServletResponse.SC_ACCEPTED);
//        if (free) {
            return new Response(operationIdentity,
                    String.format(freeCreatedTemplate, accountId));
//        } else {
//            return new Response(operationIdentity,
//                    String.format(paidCreatedTemplate, accountId));
//        }
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
        RestMessage restMessage = RestHelper.getFromJson(requestBody);
        System.out.println(restMessage.toString());

        String operationIdentity = restMessage.getOperationIdentity();

        //checking for correct operationIdentity
        if (!RestHelper.isValidOperationIdentity(restMessage.getOperationIdentity())) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return new Response("0", "Bad operationIdentity");
        }

        //check all necessary params in body
        if (!RestHelper.hasRequiredParams(restMessage, "ftp.modify")) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return new Response(operationIdentity, "Bad request");
        }

        Integer id = null;
        if (restMessage.getData().containsKey("id")) {
            id = parseInt(restMessage.getData().get("id").toString());
        } else {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return new Response("0", "Resource not specified");
        }

        RestTemplate restTemplate = new RestTemplate();
        //testing response from RC
        FtpAccountMessage rcRestResponse = restTemplate.getForObject("http://127.0.0.1:5000/rc/account/"+accountId+"/ftpAccount", FtpAccountMessage.class);
        HashMap<String, Object> ftpAccount = rcRestResponse.getFtpAccount(id);
        if (ftpAccount.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return new Response("0", String.format("ftpAccount with id %s is not found or not belongs to you", id.toString()));
        }

        //set services for feedback waiting
        ArrayList<String> requiredFeedback = new ArrayList<>();
        requiredFeedback.add("rc");
        requiredFeedback.add("fin");
        requiredFeedback.add("te");

        operation.setOperationIdentity(operationIdentity);
        operation.newOperation(requiredFeedback);

        CreateModifyMessage createModifyMessage = new CreateModifyMessage();
        createModifyMessage.setOperationIdentity(operationIdentity);
        createModifyMessage.setParams(restMessage.getData());

        amqpController.send("ftp.modify", "rc", createModifyMessage);

        //giving HTTP response
        response.setStatus(HttpServletResponse.SC_ACCEPTED);
        return new Response(operationIdentity,
                String.format(modifiedTemplate, accountId));
    }
}
