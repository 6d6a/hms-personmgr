package ru.majordomo.hms.personmgr.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;

import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;

@Service
public class MailManager {

    private final static Logger logger = LoggerFactory.getLogger(MailManager.class);

    private static final HashMap<String, String> URL_MAP;

    static {
        URL_MAP = new HashMap<>();
        URL_MAP.put("login", "/login_check");
        URL_MAP.put("sendEmail", "/newmailtask");
        URL_MAP.put("sendSms", "/smsqueue");
    }

    private RestTemplate restTemplate;
    private HttpHeaders headers;
    private String credentials;
    private String URL_ROOT;
    private String token = "";

    public MailManager(
            @Value("${mail_manager.url}") String url,
            @Value("${mail_manager.username}") String username,
            @Value("${mail_manager.password}") String password
    ) {
        restTemplate = new RestTemplate();
        headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        credentials = "{\"_username\":\"" + username + "\",\"_password\":\"" + password + "\"}";
        URL_ROOT = url;
    }

    private String getToken() {
        HttpEntity<String> entity = new HttpEntity<>(credentials, headers);
        HashMap someResponse = restTemplate.postForObject(URL_ROOT + URL_MAP.get("login"), entity, HashMap.class);
        token = (String) someResponse.get("token");
        headers.set("Authorization", "Bearer " + someResponse.get("token"));
        logger.debug("Token for mailManager: " + someResponse.get("token"));
        return someResponse.get("token").toString();
    }

    public HashMap sendEmail(SimpleServiceMessage message) throws RestClientException {
        setToken();
        HttpEntity entity = new HttpEntity<>(message.getParams(), headers);
        logger.debug("HttpEntity in mailManager sendEmail: " + entity.toString());

        HashMap<String, String> responseBody = new HashMap<>();

        try {
            ResponseEntity<HashMap> response = restTemplate.postForEntity(URL_ROOT + URL_MAP.get("sendEmail"), entity, HashMap.class);

            if (response.getStatusCode().is4xxClientError()) {
                token = "";
                sendEmail(message);
            }

            return response.getBody();
        } catch (RestClientException e) {
            e.printStackTrace();
            sendEmail(message);
            return responseBody;
        }
    }

    public HashMap sendSms(SimpleServiceMessage message) throws RestClientException {
        setToken();
        HttpEntity entity = new HttpEntity<>(message.getParams(), headers);
        logger.debug("HttpEntity in mailManager sendSms: " + entity.toString());

        HashMap<String, String> responseBody = new HashMap<>();

        try {
            ResponseEntity<HashMap> response = restTemplate.postForEntity(URL_ROOT + URL_MAP.get("sendSms"), entity, HashMap.class);

            if (response.getStatusCode().is4xxClientError()) {
                token = "";
                sendSms(message);
            }

            return response.getBody();
        } catch (RestClientException e) {
            e.printStackTrace();
            sendSms(message);
            return responseBody;
        }
    }

    private void setToken() {
        if (token.equals("")) {
            getToken();
        }
    }
}
