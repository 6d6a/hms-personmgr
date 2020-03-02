package ru.majordomo.hms.personmgr.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import ru.majordomo.hms.personmgr.config.FeignConfig;
import ru.majordomo.hms.personmgr.dto.partners.*;

import java.time.LocalDateTime;

@FeignClient(name = "partners", configuration = FeignConfig.class)
public interface PartnersFeignClient {
    @PostMapping(value = "/project/HMS/account/{accountId}/stat/register", consumes = "application/json")
    RegisterStat registerByAccountIdAndCode(@PathVariable("accountId") String accountId, @RequestBody RegisterStatRequest registerStatRequest);

    @PostMapping(value = "/project/HMS/account/{accountId}/stat/action", consumes = "application/json")
    ActionStat actionByAccountIdAndAmount(@PathVariable("accountId") String accountId, @RequestBody ActionStatRequest actionStatRequest);

    @GetMapping(value = "/{accountId}/code", consumes = "application/json")
    Code getHmsAccountCode(@PathVariable("accountId") String accountId);

    @PostMapping(value = "/project/HMS/account/{accountId}/code")
    String createCode(
            @PathVariable String accountId,
            @RequestParam String accountName,
            @RequestParam String code,
            @RequestParam LocalDateTime created
    );

}
