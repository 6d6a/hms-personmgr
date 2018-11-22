package ru.majordomo.hms.personmgr.feign;

import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import ru.majordomo.hms.personmgr.config.FeignConfig;

import java.util.Map;

@FeignClient(name = "YANDEX-PROMOTER", configuration = FeignConfig.class)
public interface YaPromoterFeignClient {
    @PostMapping(value = "/register/HMS/{accountId}", consumes = "application/json")
    Object registerEvent(@PathVariable("accountId") String accountId, @RequestBody Map<String, String> request);

    @PostMapping(value = "/payment/HMS/{accountId}", consumes = "application/json")
    Object paymentEvent(@PathVariable("accountId") String accountId, @RequestBody Map<String, Object> request);
}
