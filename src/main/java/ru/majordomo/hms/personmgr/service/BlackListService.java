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

    @Value("${blacklist.control_url}")
    public void setControlBlackListUrl(String controlBlackListUrl) {
        this.controlBlackListUrl = controlBlackListUrl;
    }

    public Boolean domainExistsInControlBlackList(String domain) {
        Boolean exists = false;
        try {
            exists = restTemplate.getForObject(controlBlackListUrl + "/" + domain, Boolean.class);
            if (exists == null) {
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Exception in BlackListService.domainExistsInControlBlackList " + e.getMessage());
        }

        return exists;
    }
}
