package ru.majordomo.hms.personmgr.service;

import org.springframework.cloud.netflix.feign.FeignClient;
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
}
