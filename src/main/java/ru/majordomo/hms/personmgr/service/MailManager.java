package ru.majordomo.hms.personmgr.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;

import ru.majordomo.hms.personmgr.common.message.MailManagerMessage;

@Service
public class MailManager {

    private static final HashMap<String, String> URL_MAP;

    static {
        URL_MAP = new HashMap<>();
        URL_MAP.put("login", "/login_check");
        URL_MAP.put("addmail", "/newmailtask");
    }

    private RestTemplate restTemplate;
    private HttpHeaders headers;
    private String credentials;
    private String URL_ROOT;

    public MailManager(@Value("${mail_manager.url}") String url, @Value("${mail_manager.username}") String username, @Value("${mail_manager.password}") String password) {
        restTemplate = new RestTemplate();
        headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        credentials = "{\"_username\":\"" + username + "\",\"_password\":\"" + password + "\"}";
        URL_ROOT = url;
        getToken();
    }

    private String getToken() {
        HttpEntity<String> entity = new HttpEntity<>(credentials, headers);
        HashMap someResponse = restTemplate.postForObject(URL_ROOT + URL_MAP.get("login"), entity, HashMap.class);
        headers.set("Authorization", "Bearer " + someResponse.get("token"));
        return someResponse.get("token").toString();
    }

    public HashMap send(MailManagerMessage message) {
        HttpEntity entity = new HttpEntity<>(message, headers);
        return restTemplate.postForObject(URL_ROOT + URL_MAP.get("addmail"), entity, HashMap.class);
    }
}
