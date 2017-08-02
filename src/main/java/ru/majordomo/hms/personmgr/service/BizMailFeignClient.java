package ru.majordomo.hms.personmgr.service;

import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import ru.majordomo.hms.personmgr.config.FeignConfig;

import java.util.List;

@FeignClient(name = "bizmail", configuration = FeignConfig.class)
public interface BizMailFeignClient {
    @RequestMapping(method = RequestMethod.GET, value = "/{accountId}/domain", consumes = "application/json", headers = "X-HMS-AllDomainFromDB=true")
    List<Object> getDomainsFromBizmail(@PathVariable("accountId") String accountId);
}