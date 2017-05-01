package ru.majordomo.hms.personmgr.service;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
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

        JSONObject credentialsJson = new JSONObject();
        try {
            credentialsJson.put("_username", username);
            credentialsJson.put("_password", password);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        credentials = credentialsJson.toString();
        URL_ROOT = url;
    }

    private String getToken() {
        HttpEntity<String> entity = new HttpEntity<>(credentials, headers);
        ResponseEntity<HashMap> someResponse = restTemplate.postForEntity(
                URL_ROOT + URL_MAP.get("login"),
                entity,
                HashMap.class
        );
        token = (String) someResponse.getBody().get("token");
        headers.set("Authorization", "Bearer " + token);
        logger.debug("Token for mailManager: " + token);
        return token;
    }

    public HashMap sendEmail(SimpleServiceMessage message) throws RestClientException {
        checkToken();
        HttpEntity entity = new HttpEntity<>(message.getParams(), headers);
        logger.debug("HttpEntity in mailManager sendEmail: " + entity.toString());

        HashMap<String, String> responseBody = new HashMap<>();

        try {
            ResponseEntity<HashMap> response = restTemplate.postForEntity(
                    URL_ROOT + URL_MAP.get("sendEmail"),
                    entity,
                    HashMap.class
            );

            return response.getBody();
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                unSetToken();
                sendEmail(message);
            }
        } catch (RestClientException e) {
            e.printStackTrace();
        }

        return responseBody;
    }

    public HashMap sendSms(SimpleServiceMessage message) throws RestClientException {
        checkToken();
        HttpEntity entity = new HttpEntity<>(message.getParams(), headers);
        logger.debug("HttpEntity in mailManager sendSms: " + entity.toString());

        HashMap<String, String> responseBody = new HashMap<>();

        try {
            ResponseEntity<HashMap> response = restTemplate.postForEntity(
                    URL_ROOT + URL_MAP.get("sendSms"),
                    entity,
                    HashMap.class
            );

            if (response.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                unSetToken();
                sendSms(message);
            }

            return response.getBody();
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                unSetToken();
                sendEmail(message);
            }
        } catch (RestClientException e) {
            e.printStackTrace();
        }

        return responseBody;
    }

    private void checkToken() {
        if (token.equals("")) {
            getToken();
        }
    }

    private void unSetToken() {
        token = "";
    }
}
