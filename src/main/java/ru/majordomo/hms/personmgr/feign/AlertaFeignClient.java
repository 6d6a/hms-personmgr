package ru.majordomo.hms.personmgr.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import ru.majordomo.hms.personmgr.config.AlertaFeignConfig;
import ru.majordomo.hms.personmgr.dto.alerta.AlertDto;
import ru.majordomo.hms.personmgr.dto.alerta.AlertaPageResponse;
import ru.majordomo.hms.personmgr.dto.alerta.AlertaResponse;
import ru.majordomo.hms.personmgr.dto.alerta.AlertaResponseStatus;

import javax.annotation.Nonnull;
import java.util.List;

@FeignClient(name = "alerta", configuration = AlertaFeignConfig.class, url = "${alerta.api-url}")
public interface AlertaFeignClient {
    @PostMapping(value = "/alert", consumes = "application/json")
    AlertaResponse sendAlert(@RequestBody AlertDto alert);

    @DeleteMapping(value = "/alert/{id}")
    AlertaResponseStatus deleteAlertById(@Nonnull @PathVariable("id") String id);

    @GetMapping("/alerts")
    AlertaPageResponse searchAlertsByEnvironment(@Nonnull @RequestParam("environment") String environment);
}
