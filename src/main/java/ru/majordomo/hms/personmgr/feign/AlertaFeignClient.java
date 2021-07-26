package ru.majordomo.hms.personmgr.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import ru.majordomo.hms.personmgr.config.AlertaFeignConfig;
import ru.majordomo.hms.personmgr.dto.alerta.AlertDto;
import ru.majordomo.hms.personmgr.dto.alerta.AlertaResponse;

@FeignClient(name = "alerta", configuration = AlertaFeignConfig.class, url = "${alerta.api-url}")
public interface AlertaFeignClient {
    @PostMapping (value = "/alert", consumes = "application/json")
    AlertaResponse sendAlert(@RequestBody AlertDto alert);
}
