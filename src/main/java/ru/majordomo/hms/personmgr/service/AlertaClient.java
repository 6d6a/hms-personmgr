package ru.majordomo.hms.personmgr.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import ru.majordomo.hms.personmgr.config.HmsProperties;
import ru.majordomo.hms.personmgr.dto.alerta.Alert;
import ru.majordomo.hms.personmgr.config.AlertaSettings;
import ru.majordomo.hms.personmgr.dto.alerta.AlertDto;
import ru.majordomo.hms.personmgr.dto.alerta.AlertaResponse;
import ru.majordomo.hms.personmgr.feign.AlertaFeignClient;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@ParametersAreNonnullByDefault
public class AlertaClient {
    private final AlertaSettings alertaSettings;
    private final AlertaFeignClient alertaFeignClient;
    @Value("${spring.application.name}") private final String applicationName;
    private final HmsProperties hmsProperties;
    private final Environment env;

    @Nullable
    public String send(Alert alert) {
        Assert.notNull(alert, "Alert must not be null");
        try {
            if (!alertaSettings.isEnabled()) {
                log.debug("Sending the alert was skipped because it was disabled in the configuration. Event: {}, resource: {}", alert.getEvent(), alert.getResource());
                return null;
            }
            AlertDto alertDto = convertAlert(alert);
            AlertaResponse response = alertaFeignClient.sendAlert(alertDto);
            return response.getId();
        } catch (Exception e) {
            log.error(String.format("An exception occurred while send alert. Event: %s, resource: %s", alert.getEvent(), alert.getResource()), e);
            return null;
        }
    }

    @Nonnull
    private AlertDto convertAlert(Alert alert) {
        AlertDto alertDto = new AlertDto();
        alertDto.setResource(alert.getResource());
        alertDto.setEvent(alert.getEvent().name());
        alertDto.setSeverity(alert.getSeverity());
        alertDto.setValue(alert.getValue());
        alertDto.setText(alert.getText());
        alertDto.setOrigin(applicationName);
        alertDto.setEnvironment(alertaSettings.getEnvironment());
        List<String> services = alert.getService() == null ? new ArrayList<>() : new ArrayList<>(alert.getService());
        services.add(hmsProperties.getInstance().getName());
        services.addAll(Arrays.asList(env.getActiveProfiles()));
        alertDto.setService(services);
        alertDto.setStatus(alert.getStatus());
        return alertDto;
    }
}
