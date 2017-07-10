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

    private static final HashMap<UrlKey, String> URL_MAP;

    private enum UrlKey {
        LOGIN,
        SEND_EMAIL,
        SEND_SMS
    }

    static {
        URL_MAP = new HashMap<>();
        URL_MAP.put(UrlKey.LOGIN, "/login_check");
        URL_MAP.put(UrlKey.SEND_EMAIL, "/newmailtask");
        URL_MAP.put(UrlKey.SEND_SMS, "/smsqueue");
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
            logger.error("Exception in ru.majordomo.hms.personmgr.service.MailManager.MailManager " + e.getMessage());
        }

        credentials = credentialsJson.toString();
        URL_ROOT = url;
    }

    private String getToken() {
        HttpEntity<String> entity = new HttpEntity<>(credentials, headers);
        ResponseEntity<HashMap> someResponse = restTemplate.postForEntity(
                URL_ROOT + URL_MAP.get(UrlKey.LOGIN),
                entity,
                HashMap.class
        );
        token = (String) someResponse.getBody().get("token");
        headers.set("Authorization", "Bearer " + token);
        logger.debug("Token for mailManager: " + token);
        return token;
    }

    public HashMap sendEmail(SimpleServiceMessage message) {
        return send(message, UrlKey.SEND_EMAIL);
    }

    public HashMap sendSms(SimpleServiceMessage message) {
        return send(message, UrlKey.SEND_SMS);
    }

    private HashMap send(SimpleServiceMessage message, UrlKey urlKey) {
        checkToken();
        HttpEntity entity = new HttpEntity<>(message.getParams(), headers);
        logger.debug("HttpEntity in mailManager " + urlKey.name() + ": " + entity.toString());

        HashMap<String, String> responseBody = new HashMap<>();

        try {
            ResponseEntity<HashMap> response = restTemplate.postForEntity(
                    URL_ROOT + URL_MAP.get(urlKey),
                    entity,
                    HashMap.class
            );

            return response.getBody();
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                unSetToken();
                send(message, urlKey);
            } else {
                e.printStackTrace();
                logger.error("Exception in ru.majordomo.hms.personmgr.service.MailManager.send #1 " + e.getMessage() + " " + e.getResponseBodyAsString());
            }
        } catch (RestClientException e) {
            e.printStackTrace();
            logger.error("Exception in ru.majordomo.hms.personmgr.service.MailManager.send #2 " + e.getMessage());
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
