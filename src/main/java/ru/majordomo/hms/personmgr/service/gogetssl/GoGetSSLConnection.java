package ru.majordomo.hms.personmgr.service.gogetssl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import ru.majordomo.hms.personmgr.config.GoGetSSLConfig;
import ru.majordomo.hms.personmgr.exception.InternalApiException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
public class GoGetSSLConnection {
    private final String url;
    private String key;

    GoGetSSLConnection(GoGetSSLConfig config) {
        this.url = config.getApiUrl();
        auth(config.getLogin(), config.getPassword());
    }

    public Map addSSLOrder(MultiValueMap<String, String> body) {
        return call("/orders/add_ssl_order/", getAuthParams(), HttpMethod.POST, body, Map.class);
    }

    public Map addSSLRenewOrder(MultiValueMap<String, String> body) {
        return call("/orders/add_ssl_renew_order/", getAuthParams(), HttpMethod.POST, body, Map.class);
    }

    public Map activateSSLOrder(String orderId) {
        return call("/orders/ssl/activate/" + orderId, getAuthParams(), HttpMethod.GET, null, Map.class);
    }

    public Map getOrderStatus(Integer orderId) {
        return call("/orders/status/" + orderId, getAuthParams(), HttpMethod.GET, null, Map.class);
    }

    public Map cancelSSLOrder(Integer orderId, String reason) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("order_id", orderId.toString());
        body.add("reason", reason);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        HttpEntity<Object> entity = new HttpEntity<>(
                body,
                headers
        );

        return call("/orders/cancel_ssl_order/", getAuthParams(), HttpMethod.POST, entity, Map.class);
    }

    private void auth(String login, String password) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("user", login);
        body.add("pass", password);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        Map response = call("/auth", null, HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);

        if (response == null || response.get("key") == null) {
            throw new InternalApiException("Failed to authenticate at GoGetSSL API");
        } else {
            this.key = (String) response.get("key");
        }
    }

    private List<Pair<String, Object>> getAuthParams() {
        if (key == null || key.isEmpty()) {
            throw new RuntimeException("Failed to authenticate at GoGetSSL API");
        }
        return Arrays.asList(Pair.of("auth_key", key));
    }

    private <T, B> T call(
            String path, Iterable<Pair<String, Object>> queryParams, HttpMethod method, B body, Class<T> responseType
    ) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        return call(path, queryParams, method, new HttpEntity<>(body, headers), responseType);
    }

    private <T, E> T call(
            String path, Iterable<Pair<String, Object>> queryParams, HttpMethod method, HttpEntity<E> entity, Class<T> responseType
    ) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url + path);

        if (queryParams != null) {
            for (Pair<String, Object> param : queryParams) {
                builder.queryParam(param.getFirst(), param.getSecond().toString());
            }
        }

        try {
            ResponseEntity<T> response = new RestTemplate().exchange(
                    builder.toUriString(),
                    method,
                    entity,
                    responseType);

            log.debug("response with code {} and body {}",
                    response.getStatusCodeValue(), response.hasBody() ? response.getBody() : "null");

            return response.getBody();
        } catch (Exception e) {
            log.error(e.getMessage());
            return null;
        }
    }
}
















