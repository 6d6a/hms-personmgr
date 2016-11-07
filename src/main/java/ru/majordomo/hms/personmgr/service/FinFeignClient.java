package ru.majordomo.hms.personmgr.service;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import ru.majordomo.hms.personmgr.common.FinService;

/**
 * FinFeignClient
 */
@FeignClient("fin")
public interface FinFeignClient {
    @RequestMapping(method = RequestMethod.POST, value = "/services", consumes = "application/json")
    FinService create(FinService finService);

    @Cacheable("services")
    @RequestMapping(method = RequestMethod.GET, value = "/services/{id}", consumes = "application/json")
    FinService get(@PathVariable("id") String id);
}
