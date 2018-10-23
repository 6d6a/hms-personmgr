package ru.majordomo.hms.personmgr.service;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;

import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.config.MailManagerConfig;

@Service
public class MailManager {

    private final static Logger logger = LoggerFactory.getLogger(MailManager.class);

    private static final HashMap<UrlKey, String> URL_MAP;

    public enum UrlKey {
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
            MailManagerConfig config
    ) {
        restTemplate = new RestTemplate();
        headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        JSONObject credentialsJson = new JSONObject();
        try {
            credentialsJson.put("_username", config.getUsername());
            credentialsJson.put("_password", config.getPassword());
        } catch (JSONException e) {
            e.printStackTrace();
            logger.error("Exception in ru.majordomo.hms.personmgr.service.MailManager.MailManager " + e.getMessage());
        }

        credentials = credentialsJson.toString();
        URL_ROOT = config.getUrl();
    }

    private String getToken() {
        HttpEntity<String> entity = new HttpEntity<>(credentials, headers);
        ResponseEntity<HashMap> someResponse = restTemplate.postForEntity(
                URL_ROOT + URL_MAP.get(UrlKey.LOGIN),
                entity,
                HashMap.class
        );
        token = someResponse != null && someResponse.getBody() != null ? (String) someResponse.getBody().get("token") : null;
        headers.set("Authorization", "Bearer " + token);
        logger.debug("Token for mailManager: " + token);
        return token;
    }

    @Recover
    public void recoverSend(HttpClientErrorException e, SimpleServiceMessage message, UrlKey urlKey) {
        logger.info("Recovering from HttpClientErrorException: " + e.getMessage());
        unSetToken();
        send(message, urlKey);
    }

    @Retryable(include = {HttpClientErrorException.class}, backoff = @Backoff(delay = 1000))
    public void send(SimpleServiceMessage message, UrlKey urlKey) {
        checkToken();
        HttpEntity entity = new HttpEntity<>(message.getParams(), headers);
        logger.debug("HttpEntity in mailManager " + urlKey.name() + ": " + entity.toString());

        restTemplate.postForEntity(
                URL_ROOT + URL_MAP.get(urlKey),
                entity,
                HashMap.class
        );
    }

    private void checkToken() {
        if (token == null || token.equals("")) {
            getToken();
        }
    }

    private void unSetToken() {
        token = "";
    }
}
