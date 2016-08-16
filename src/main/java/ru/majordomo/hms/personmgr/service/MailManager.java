package ru.majordomo.hms.personmgr.service;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;
import ru.majordomo.hms.personmgr.models.mailmanager.NewMailTask;

import java.util.HashMap;

public class MailManager {

    private RestTemplate restTemplate;
    private HttpHeaders headers;

    private static final String creds = "{\"_username\":\"api_user\",\"_password\":\"***REMOVED***\"}";
    private static final String URL_ROOT = "http://mail-manager.intr/api";
    private static final HashMap<String, String> URL_MAP;
    static {
        URL_MAP = new HashMap<>();
        URL_MAP.put("login", "/login_check");
        URL_MAP.put("addmail", "/newmailtask");
    }

    public MailManager() {
        restTemplate = new RestTemplate();
        headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        getToken();
    }

    private String getToken() {
        HttpEntity<String> entity = new HttpEntity<>(creds, headers);
        HashMap someResponse = restTemplate.postForObject(URL_ROOT + URL_MAP.get("login"), entity, HashMap.class);
        headers.set("Authorization", "Bearer " + someResponse.get("token"));
        return someResponse.get("token").toString();
    }

    public HashMap createTask(NewMailTask mailTask) {
        HttpEntity entity = new HttpEntity<>(mailTask, headers);
        return restTemplate.postForObject(URL_ROOT + URL_MAP.get("addmail"), entity, HashMap.class);
    }
}
