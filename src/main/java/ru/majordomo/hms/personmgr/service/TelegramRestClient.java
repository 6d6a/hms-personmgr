package ru.majordomo.hms.personmgr.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.support.BasicAuthorizationInterceptor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import ru.majordomo.hms.personmgr.dto.TelegramMessageData;

@Component
public class TelegramRestClient {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Value("${telegram.telegramUri}")
    private String apiUri;

    @Value("${telegram.telegramUsername}")
    private String login;

    @Value("${telegram.telegramPassword}")
    private String password;

    private final RestTemplate restTemplate;

    public TelegramRestClient() {
        this.restTemplate = new RestTemplate();
    }

    public void callVoidApi(String uriPostfix) {
        callApi(uriPostfix, HttpMethod.POST, null, Void.class);
    }

    public void callSendTelegramMessage(TelegramMessageData requestBody) {
        HttpEntity<TelegramMessageData> request = new HttpEntity<>(requestBody);
        callApi("/send", HttpMethod.POST, request, null);
    }

    public String callGetTelegramBotName() {
        return callApi("/botname", HttpMethod.GET, null, String.class);
    }

    private <T> T callApi(String uriPostfix, HttpMethod httpMethod, HttpEntity<?> requestEntity, Class<T> responseClass) {
        ResponseEntity<T> responseEntity;

        try {
            restTemplate.getInterceptors().add(
                    new BasicAuthorizationInterceptor(login, password));
            responseEntity = restTemplate.exchange(apiUri + uriPostfix, httpMethod, requestEntity, responseClass);

            if (!responseEntity.getStatusCode().is2xxSuccessful()) {
                logger.error("Ошибка при выполнении запроса к " + apiUri + uriPostfix +
                        " StatusCode: " + responseEntity.getStatusCode() +
                        "Body: " + responseEntity.getBody()
                );
            }
        } catch (Exception e) {
            logger.error("Ошибка при выполнении запроса к " + apiUri + uriPostfix +
                    " Exception: " + e.getMessage()
            );

            throw e;
        }

        return responseEntity.getBody();
    }
}
