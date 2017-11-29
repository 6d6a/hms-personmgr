package ru.majordomo.hms.personmgr.service;

import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import ru.majordomo.hms.personmgr.config.FeignConfig;

@FeignClient(name = "appscat", configuration = FeignConfig.class)
public interface AppscatFeignClient {
    @RequestMapping(method = RequestMethod.GET, value = "/{accountId}/app_install/is_pending/{websiteId}", consumes = "application/json")
    Boolean isPendingInstallForAccountByWebSiteId(@PathVariable("accountId") String accountId, @PathVariable("websiteId") String websiteId);
}
