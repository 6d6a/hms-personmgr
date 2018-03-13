package ru.majordomo.hms.personmgr.service;

import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.config.FeignConfig;

import java.util.Map;

@FeignClient(name = "stat", configuration = FeignConfig.class)
public interface StatFeignClient {
    @RequestMapping(method = RequestMethod.POST, value = "/collector/notifications/increment", consumes = "application/json")
    void sendNotificaton(Map<String, Object> body);

    @RequestMapping(method = RequestMethod.POST, value = "/collector/payment-after-notification/increment", consumes = "application/json")
    void paymentAfterNotificationIncrement(Map<String, Object> body);
}
