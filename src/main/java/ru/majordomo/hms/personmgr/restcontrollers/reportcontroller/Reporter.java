package ru.majordomo.hms.personmgr.restcontrollers.reportcontroller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.majordomo.hms.personmgr.models.operation.Operation;
import ru.majordomo.hms.personmgr.models.Response;
import ru.majordomo.hms.personmgr.restcontrollers.RestHelper;

import javax.servlet.http.HttpServletResponse;
import java.util.Map;

@RestController
public class Reporter {

    @Autowired
    private Operation operation;

    /**
     * Fetches operation state from Redis
     *
     * @param operationIdentity - operation ID
     * @param response          - HTTP Servlet
     * @return API Response
     */
    @RequestMapping(value = "/pm/account/checkOperation", method = RequestMethod.GET)
    public Response checkOperation(@RequestParam(value = "operationIdentity", defaultValue = "0") String operationIdentity,
                                   HttpServletResponse response) {
        //checking for correct operationIdentity
        if (!RestHelper.isValidOperationIdentity(operationIdentity)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return new Response("0", "Bad operationIdentity");
        } else {
            operation.setOperationIdentity(operationIdentity);
            Map<Object, Object> currentState;
            try {
                currentState = operation.fetchOperation();
            } catch (NullPointerException e) {
                e.printStackTrace();
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return new Response(operationIdentity, "No such operation");
            }
            if (!operation.isSuccess()) {
//                try {
//                    if (currentState.get("rc").toString().equals("new") ||
//                            currentState.get("fin").toString().equals("new") ||
//                            currentState.get("te").toString().equals("new")) {
                        response.setStatus(HttpServletResponse.SC_OK);
                        return new Response(operationIdentity, "new");
//                    }
//                } catch (NullPointerException e) {
//                    response.setStatus(HttpServletResponse.SC_OK);
//                    return new Response(operationIdentity, "new");
//                }
//            } else {
//                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
//                return new Response(operationIdentity, "No such operation");
            }

            response.setStatus(HttpServletResponse.SC_OK);
            return new Response(operationIdentity, operation.getReport());
        }
    }
}
