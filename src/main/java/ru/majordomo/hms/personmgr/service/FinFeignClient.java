package ru.majordomo.hms.personmgr.service;

import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Map;

import ru.majordomo.hms.personmgr.FeignConfig;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;

@FeignClient(name = "fin", fallback = FinFeignClientFallback.class, configuration = FeignConfig.class)
public interface FinFeignClient {
    @RequestMapping(method = RequestMethod.POST, value = "/payment_integration/add_payment", consumes = "application/json")
    String addPayment(Map<String, Object> payment);

    @RequestMapping(method = RequestMethod.POST, value = "/{accountId}/payment_operations/charge", consumes = "application/json")
    SimpleServiceMessage charge(@PathVariable("accountId") String accountId, Map<String, Object> paymentOperation);

    @RequestMapping(method = RequestMethod.POST, value = "/{accountId}/payment_operations/block", consumes = "application/json")
    SimpleServiceMessage block(@PathVariable("accountId") String accountId, Map<String, Object> paymentOperation);

    @RequestMapping(method = RequestMethod.DELETE, value = "/{accountId}/payment_operations/{documentNumber}", consumes = "application/json")
    SimpleServiceMessage unblock(@PathVariable("accountId") String accountId, @PathVariable("documentNumber") String documentNumber);

    @RequestMapping(method = RequestMethod.POST, value = "/{accountId}/payment_operations/{documentNumber}/charge", consumes = "application/json")
    SimpleServiceMessage chargeBlocked(@PathVariable("accountId") String accountId, @PathVariable("documentNumber") String documentNumber);

    @RequestMapping(method = RequestMethod.GET, value = "/{accountId}/balance", consumes = "application/json")
    Map<String, Object> getBalance(@PathVariable("accountId") String accountId);
}
