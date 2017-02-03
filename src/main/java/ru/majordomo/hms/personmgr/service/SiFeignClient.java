package ru.majordomo.hms.personmgr.service;

import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;

@FeignClient(name = "si")
public interface SiFeignClient {
    @RequestMapping(method = RequestMethod.POST, value = "/web-access-accounts/create_from_message", consumes = "application/json")
    SimpleServiceMessage createWebAccessAccount(SimpleServiceMessage message);
}
