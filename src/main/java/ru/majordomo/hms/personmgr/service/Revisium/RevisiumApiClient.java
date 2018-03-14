package ru.majordomo.hms.personmgr.service.Revisium;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.support.HttpRequestWrapper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import ru.majordomo.hms.personmgr.dto.revisium.CheckResponse;
import ru.majordomo.hms.personmgr.dto.revisium.GetResultResponse;
import ru.majordomo.hms.personmgr.dto.revisium.GetStatResponse;

import java.io.IOException;
import java.net.IDN;
import java.net.URI;
import java.util.Collections;
import java.util.List;

@Service
public class RevisiumApiClient {

    private String serverUrl;
    private String key;
    private RestTemplate restTemplate;

    protected Logger logger = LoggerFactory.getLogger(getClass());

    private final static String VERSION = "1.2.0";

    @Autowired
    public RevisiumApiClient(
            @Value("${revisium.key}") String key,
            @Value("${revisium.url}") String serverUrl
    ) {

        this.serverUrl = serverUrl;
        this.key = key;

        restTemplate = new RestTemplate();
        List<ClientHttpRequestInterceptor> interceptors = restTemplate.getInterceptors();
//        interceptors.add(new RevisiumApiHttpRequestInterceptor(key));
        restTemplate.setInterceptors(interceptors);
    }

    public GetStatResponse getStat() {

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(serverUrl)
                .queryParam("action", "get_stat")
                .queryParam("key", key)
                .queryParam("api_ver", VERSION);

        GetStatResponse getStatResponse = restTemplate.getForObject(builder.build().encode().toUri(), GetStatResponse.class);

        if (getStatResponse.getErrorMessage() != null && !getStatResponse.getErrorMessage().equals("")) {
            logger.error("Ошибка при запросе иформации об аккаунте: " + getStatResponse.getErrorMessage());
        }

        return getStatResponse;
    }

    /*
      Проверка сайта занимает около минуты
    */
    public CheckResponse check(String siteUrl) {

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(serverUrl)
                .queryParam("action", "check")
                .queryParam("url", IDN.toASCII(siteUrl))
                .queryParam("key", key)
                .queryParam("api_ver", VERSION);

        CheckResponse checkResponse = restTemplate.getForObject(builder.build().encode().toUri(), CheckResponse.class);

        if (checkResponse.getErrorMessage() != null && !checkResponse.getErrorMessage().equals("")) {
            logger.error("Ошибка при запросе проверки сайта: " + checkResponse.getErrorMessage());
        }

        return checkResponse;
    }

    public GetResultResponse getResult(String requestId) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(serverUrl)
                .queryParam("action", "get_result")
                .queryParam("request_id", requestId)
                .queryParam("key", key)
                .queryParam("api_ver", VERSION);

        GetResultResponse getResultResponse = restTemplate.getForObject(builder.build().encode().toUri(), GetResultResponse.class);

        if (getResultResponse.getErrorMessage() != null && !getResultResponse.getErrorMessage().equals("")) {
            logger.error("Ошибка при запросе результата проверки сайта: " + getResultResponse.getErrorMessage());
        }

        return getResultResponse;
    }

    //TODO revisium (in future) Разобраться почему не работает
//    class RevisiumApiHttpRequestInterceptor implements ClientHttpRequestInterceptor {
//
//        private final String key;
//
//        RevisiumApiHttpRequestInterceptor(String key) {
//            this.key = key;
//        }
//
//        @Override
//        public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
//            HttpHeaders httpHeaders = request.getHeaders();
//            httpHeaders.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
//
//            HttpRequest requestWrapper = new HttpRequestWrapper(request) {
//                @Override
//                public URI getURI() {
//                    URI uri = super.getURI();
//
//                    UriComponentsBuilder builder = UriComponentsBuilder.fromUri(uri);
//                    builder.replaceQueryParam("key", key);
//                    builder.replaceQueryParam("api_ver", VERSION);
//
//                    UriComponents uriComponents = builder.build(true);
//
//                    logger.error(uriComponents.toUri().toString());
//
//                    return uriComponents.toUri();
//                }
//            };
//
//            return execution.execute(requestWrapper, body);
//        }
//    }
}
