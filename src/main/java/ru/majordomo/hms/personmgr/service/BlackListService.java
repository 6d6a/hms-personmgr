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

    @Value("${blacklist.control_url}")
    private String controlBlackListUrl;

    @Value("${blacklist.control2_url}")
    private String control2BlackListUrl;

    @Value("${blacklist.mailbox_url}")
    private String mailboxBlackListUrl;

    /**
     * @return true если домен заблокирован через blacklist, false если нет или сервис blacklist недоступен
     */
    public boolean domainExistsInControlBlackList(String domain) {

        try {
            Boolean exists = restTemplate.getForObject(controlBlackListUrl + "/" + domain, Boolean.class);
            if (exists != null && exists) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Exception in BlackListService.domainExistsInControlBlackList " + e.getMessage());
        }

        try {
            Boolean exists = restTemplate.getForObject(control2BlackListUrl + "/" + domain, Boolean.class);
            if (exists != null && exists) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Exception in BlackListService.domainExistsInControl2BlackList " + e.getMessage());
        }

        return false;
    }

    /**
     * @return true если почтовый ящик заблокирован через blacklist, false если нет или сервис blacklist недоступен
     */
    public boolean mailBoxExistsInBlackList(String mailboxFullName) {
        try {
            Boolean exists = restTemplate.getForObject(mailboxBlackListUrl + "/" + mailboxFullName, Boolean.class);
            if (exists != null && exists) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Exception in BlackListService.mailBoxExistsInBlackList " + e.getMessage());
        }

        return false;
    }
}
