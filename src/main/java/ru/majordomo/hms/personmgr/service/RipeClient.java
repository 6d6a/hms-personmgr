package ru.majordomo.hms.personmgr.service;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;

import lombok.extern.slf4j.Slf4j;
import ru.majordomo.hms.personmgr.dto.stat.RipeStatResponse;

@Slf4j
@Service
public class RipeClient {
    private final RestTemplate restTemplate = new RestTemplate();

    @Cacheable("asnByAddress")
    public String getAsnByAddress(String address) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        HttpEntity<String> entity = new HttpEntity<>(headers);

        String uri = "https://stat.ripe.net/data/network-info/data.json?resource=" + address;
        log.debug(uri);

        try {
            RipeStatResponse body = restTemplate.exchange(uri, HttpMethod.GET, entity, RipeStatResponse.class).getBody();

            if (body != null && body.getData().getAsns() != null && !body.getData().getAsns().isEmpty()) {
                return "AS" + body.getData().getAsns().get(0);
            }
        } catch (HttpClientErrorException e) {
            log.error("e {} message {} body {}", e.getClass(), e.getMessage(), e.getResponseBodyAsString());
        }

        return null;
    }

    @Cacheable("holderByAsn")
    public String getHolderByAsn(String asn) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        HttpEntity<String> entity = new HttpEntity<>(headers);
        String uri = "https://stat.ripe.net/data/as-overview/data.json?resource=" + asn;
        log.debug(uri);

        try {
            RipeStatResponse body = restTemplate.exchange(uri, HttpMethod.GET, entity, RipeStatResponse.class).getBody();

            if (body != null && body.getData().getHolder() != null) {
                return body.getData().getHolder();
            }
        } catch (HttpClientErrorException e) {
            log.error("e {} message {} body {}", e.getClass(), e.getMessage(), e.getResponseBodyAsString());
        }

        return null;
    }
}
