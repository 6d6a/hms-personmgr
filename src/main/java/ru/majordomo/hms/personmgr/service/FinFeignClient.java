package ru.majordomo.hms.personmgr.service;

import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Map;

@FeignClient(name = "fin", fallback = FinFeignClientFallback.class)
public interface FinFeignClient {
    @RequestMapping(method = RequestMethod.POST, value = "/payment_integration/add_payment", consumes = "application/json")
    Map<String, Object> addPayment(Map<String, Object> payment);

    @RequestMapping(method = RequestMethod.POST, value = "/{accountId}/payment_operations/charge", consumes = "application/json")
    Map<String, Object> charge(@PathVariable("accountId") String accountId, Map<String, Object> paymentOperation);

    @RequestMapping(method = RequestMethod.GET, value = "/{accountId}/balance", consumes = "application/json")
    Map<String, Object> getBalance(@PathVariable("accountId") String accountId);
}
