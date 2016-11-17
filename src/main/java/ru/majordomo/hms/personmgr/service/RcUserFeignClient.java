package ru.majordomo.hms.personmgr.service;

import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import ru.majordomo.hms.personmgr.common.Count;

@FeignClient(name = "rc-user", fallback = RcUserFeignClientFallback.class)
public interface RcUserFeignClient {
    @RequestMapping(method = RequestMethod.GET, value = "/{accountId}/database/count", consumes = "application/json")
    Count getDatabaseCount(@PathVariable("accountId") String accountId);

    @RequestMapping(method = RequestMethod.GET, value = "/{accountId}/website/count", consumes = "application/json")
    Count getWebsiteCount(@PathVariable("accountId") String accountId);

    @RequestMapping(method = RequestMethod.GET, value = "/{accountId}/ftp-user/count", consumes = "application/json")
    Count getFtpUserCount(@PathVariable("accountId") String accountId);
}
