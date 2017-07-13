package ru.majordomo.hms.personmgr.service;

import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import ru.majordomo.hms.personmgr.common.AvailabilityInfo;
import ru.majordomo.hms.personmgr.config.FeignConfig;

@FeignClient(name = "domain-registrar", configuration = FeignConfig.class)
public interface DomainRegistrarFeignClient {
    @RequestMapping(method = RequestMethod.GET, value = "/domain/{domainName}/availability-info", consumes = "application/json")
    AvailabilityInfo getAvailabilityInfo(@PathVariable("domainName") String domainName);
}