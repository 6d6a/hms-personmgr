package ru.majordomo.hms.personmgr.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import ru.majordomo.hms.personmgr.config.FeignConfig;

import java.util.Map;

@FeignClient(name = "stat", configuration = FeignConfig.class)
public interface StatFeignClient {
    @RequestMapping(method = RequestMethod.POST, value = "/collector/notifications/increment", consumes = "application/json")
    void notificatonWasSendIncrement(Map<String, Object> body);

    @RequestMapping(method = RequestMethod.POST, value = "/collector/payment-after-notification/increment", consumes = "application/json")
    void paymentAfterNotificationIncrement(Map<String, Object> body);

    @RequestMapping(method = RequestMethod.POST, value = "/collector/user-disabled-service-after-notification/increment", consumes = "application/json")
    void userDisabledServiceAfterNotificationIncrement(Map<String, Object> body);

    @RequestMapping(method = RequestMethod.POST, value = "/collector/unsubscribe", consumes = "application/json")
    void saveUnsubscribeStat(Map<String, String> body);
}
