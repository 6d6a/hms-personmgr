package ru.majordomo.hms.personmgr.service;

import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.config.FeignConfig;

@FeignClient(name = "stat", configuration = FeignConfig.class)
public interface StatFeignClient {
    @RequestMapping(method = RequestMethod.POST, value = "/collectors/{type}", consumes = "application/json")
    SimpleServiceMessage sendNotificaton(@PathVariable("type") String type);
}
