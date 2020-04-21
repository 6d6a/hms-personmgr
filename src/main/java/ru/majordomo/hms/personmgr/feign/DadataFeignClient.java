package ru.majordomo.hms.personmgr.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import ru.majordomo.hms.personmgr.config.DadataFeignConfig;
import ru.majordomo.hms.personmgr.dto.dadata.DadataQuery;

@FeignClient(name = "dadata", configuration = DadataFeignConfig.class, url = "${dadata.baseurl}")
public interface DadataFeignClient {

    @RequestMapping(method = RequestMethod.POST, value = "/bank", consumes = "application/json")
    public String findByBic(@RequestBody DadataQuery queryBic);

    @RequestMapping(method = RequestMethod.POST, value = "/party", consumes = "application/json")
    public String findByInn(@RequestBody DadataQuery queryBic);
}
