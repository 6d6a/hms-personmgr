package ru.majordomo.hms.personmgr.service;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Map;

import ru.majordomo.hms.personmgr.common.FinService;

/**
 * FinFeignClient
 */
@FeignClient("fin")
public interface FinFeignClient {
    @RequestMapping(method = RequestMethod.POST, value = "/services", consumes = "application/json")
    FinService createService(FinService finService);

    @Cacheable("services")
    @RequestMapping(method = RequestMethod.GET, value = "/services/{id}", consumes = "application/json")
    FinService get(@PathVariable("id") String id);

    @RequestMapping(method = RequestMethod.POST, value = "/payment_integration/add_payment", consumes = "application/json")
    Map<String, Object> addPayment(Map<String, Object> payment);

    @RequestMapping(method = RequestMethod.DELETE, value = "/{accountId}/accountServices/deleteByServiceId/{serviceId}", consumes = "application/json")
    Map<String, Object> deleteAccountServiceByServiceId(@PathVariable("accountId") String accountId, @PathVariable("serviceId") String serviceId);

    @RequestMapping(method = RequestMethod.POST, value = "/{accountId}/accountServices/addByServiceId/{serviceId}", consumes = "application/json")
    Map<String, Object> addAccountServiceByServiceId(@PathVariable("accountId") String accountId, @PathVariable("serviceId") String serviceId);
}
