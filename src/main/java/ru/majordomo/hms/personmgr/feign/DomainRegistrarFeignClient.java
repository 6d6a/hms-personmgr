package ru.majordomo.hms.personmgr.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import ru.majordomo.hms.personmgr.common.AvailabilityInfo;
import ru.majordomo.hms.personmgr.config.FeignConfig;

import java.math.BigDecimal;

@FeignClient(name = "domain-registrar", configuration = FeignConfig.class)
public interface DomainRegistrarFeignClient {
    @RequestMapping(method = RequestMethod.GET, value = "/domain/{domainName}/availability-info", consumes = "application/json")
    AvailabilityInfo getAvailabilityInfo(@PathVariable("domainName") String domainName);

    @RequestMapping(method = RequestMethod.GET, value = "/domain/{domainName}/get-renew-premium-price", consumes = "application/json")
    BigDecimal getRenewPremiumPrice(@PathVariable("domainName") String domainName);

    @RequestMapping(method = RequestMethod.POST, value = "/person/{nicHandle}/transfer/request/domain/{domain}", consumes = "application/json")
    void transferRequest(@PathVariable("nicHandle") String nicHandle, @RequestBody String authInfo, @PathVariable("domain") String domain);

    @RequestMapping(method = RequestMethod.POST, value = "/person/{nicHandle}/transfer/confirmation/{verificationCode}", consumes = "application/json")
    void transferConfirmation(@PathVariable("nicHandle") String nicHandle, @PathVariable("verificationCode") String verificationCode);
}
