package ru.majordomo.hms.personmgr.service;

import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import ru.majordomo.hms.personmgr.config.FeignConfig;
import ru.majordomo.hms.personmgr.dto.partners.ActionStat;
import ru.majordomo.hms.personmgr.dto.partners.ActionStatRequest;
import ru.majordomo.hms.personmgr.dto.partners.RegisterStat;
import ru.majordomo.hms.personmgr.dto.partners.RegisterStatRequest;

@FeignClient(name = "partners", configuration = FeignConfig.class)
public interface PartnersFeignClient {
    @PostMapping(value = "/project/HMS/account/{accountId}/stat/register", consumes = "application/json")
    RegisterStat registerByAccountIdAndCode(@PathVariable("accountId") String accountId, @RequestBody RegisterStatRequest registerStatRequest);

    @PostMapping(value = "/project/HMS/account/{accountId}/stat/action", consumes = "application/json")
    ActionStat actionByAccountIdAndAmount(@PathVariable("accountId") String accountId, @RequestBody ActionStatRequest actionStatRequest);
}
