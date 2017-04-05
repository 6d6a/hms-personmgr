package ru.majordomo.hms.personmgr.service;

import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Map;

import ru.majordomo.hms.personmgr.FeignConfig;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;

@FeignClient(name = "si", configuration = FeignConfig.class)
public interface SiFeignClient {
    @RequestMapping(method = RequestMethod.POST, value = "/web-access-accounts/create_from_message", consumes = "application/json")
    SimpleServiceMessage createWebAccessAccount(SimpleServiceMessage message);

    @RequestMapping(method = RequestMethod.POST, value = "/web-access-accounts/{accountId}/change_password", consumes = "application/json")
    SimpleServiceMessage changePassword(@PathVariable("accountId") String accountId, Map<String, String> params);
}
