package ru.majordomo.hms.personmgr.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class BlackListService {
    private RestTemplate restTemplate = new RestTemplate();
    private String fooControlBlackListUrl = "http://78.108.80.183:8080/control";
//    private String fooControlBlackListUrl = "http://localhost:8080/control";

    public Boolean domainExistsInControlBlackList(String domain) {
        Boolean exists = false;
        try {
            exists = restTemplate.getForObject(fooControlBlackListUrl + "/" + domain, Boolean.class);
            if (exists == null) {
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return exists;
    }
}