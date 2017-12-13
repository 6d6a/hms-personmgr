package ru.majordomo.hms.personmgr.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RefreshScope
public class BlackListService {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private RestTemplate restTemplate = new RestTemplate();

    private String controlBlackListUrl;
    private String control2BlackListUrl;

    @Value("${blacklist.control_url}")
    public void setControlBlackListUrl(String controlBlackListUrl) {
        this.controlBlackListUrl = controlBlackListUrl;
    }

    @Value("${blacklist.control2_url}")
    public void setControl2BlackListUrl(String control2BlackListUrl) {
        this.control2BlackListUrl = control2BlackListUrl;
    }

    public Boolean domainExistsInControlBlackList(String domain) {
        Boolean exists = false;
        try {
            exists = restTemplate.getForObject(controlBlackListUrl + "/" + domain, Boolean.class);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Exception in BlackListService.domainExistsInControlBlackList " + e.getMessage());
        }

        try {
            exists = restTemplate.getForObject(control2BlackListUrl + "/" + domain, Boolean.class);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Exception in BlackListService.domainExistsInControl2BlackList " + e.getMessage());
        }

        return exists;
    }
}
