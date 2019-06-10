package ru.majordomo.hms.personmgr.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

@Slf4j
@Service
public class RipeClient {

    private final RestTemplate restTemplate = new RestTemplate();

    public ResponseEntity<Map> search(InetAddress address) {
        String start = address.getHostAddress().replaceAll("\\.\\d+$", ".0");
        String end = address.getHostAddress().replaceAll("\\.\\d+$", ".255");
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        HttpEntity<String> entity = new HttpEntity<>(headers);
        String uri = "http://rest.db.ripe.net/ripe/inetnum/" + start + " - " + end + "?unfiltered";
        log.error(uri);
        try {
            return restTemplate.exchange(uri, HttpMethod.GET, entity, Map.class);
        } catch (HttpClientErrorException e) {
            log.error("e {} messgae {} body {}", e.getClass(), e.getMessage(), e.getResponseBodyAsString());
            return ResponseEntity.ok().build();
        }
    }

    public static void main(String[] args) throws UnknownHostException, JsonProcessingException {
//        System.out.println(InetAddress.getByName("255.255.255.255").getHostAddress());
//        new RipeClient().search(InetAddress.getByName("0.0.0.0"));
        Map body = new RipeClient().search(InetAddress.getByName("87.236.16.95"))
                .getBody();

        System.out.println(
                new ObjectMapper().writeValueAsString(body)
        );
//        new RipeClient().search(InetAddress.getByName("8.8.8.8"));
    }
}
