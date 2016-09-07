package ru.majordomo.hms.personmgr.controller.rest;


import org.codehaus.jackson.map.ObjectMapper;
import org.hibernate.validator.constraints.NotEmpty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.validation.constraints.NotNull;

import ru.majordomo.hms.personmgr.common.message.rest.RestMessage;

public abstract class RestHelper {

    private final static Logger logger = LoggerFactory.getLogger(RestHelper.class);

    private static final Map<String, ArrayList<String>> requiredParams;
    static {
        requiredParams = new HashMap<>();
        ArrayList<String> params = new ArrayList<>();
        params.add("name");
        params.add("password");
        params.add("homedir");
        params.add("accId");
        requiredParams.put("ftp.create", params);

        params = new ArrayList<>();
        params.add("name");
        params.add("planId");
        params.add("email");
        requiredParams.put("account.create", params);

        params = new ArrayList<>();
        params.add("name");
        params.add("accessObjectIds");
        params.add("accId");
        params.add("password");
        requiredParams.put("webAccessAccount.create", params);

        params = new ArrayList<>();
        params.add("name");
        params.add("dbIds");
        params.add("accId");
        params.add("password");
        requiredParams.put("DBAccount.create", params);
    }

    public static Boolean isValidOperationIdentity(String operationIdentity) {
        //TODO do some real validation
        return (!operationIdentity.equals("0"));
    }

    public static RestMessage getFromJson(String jsonString) {
        ObjectMapper mapper = new ObjectMapper();
        RestMessage restMessage = new RestMessage();
        try {
            restMessage = mapper.readValue(jsonString, RestMessage.class);
        } catch (IOException e) {
            logger.info(e.getMessage());
        }
//        if (restMessage.getOperationIdentity() == null) {
//            restMessage.setOperationIdentity("0");
//        }
        return restMessage;
    }

    public static Boolean hasRequiredParams(@NotEmpty RestMessage restMessage, @NotNull String key) {
        for (String param : requiredParams.get(key)) {
            if (!restMessage.getParams().containsKey(param)) {
                return false;
            }
        }
        return true;
    }
}
