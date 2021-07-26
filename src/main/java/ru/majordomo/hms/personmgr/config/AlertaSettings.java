package ru.majordomo.hms.personmgr.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "alerta")
public class AlertaSettings {
    private String apiUrl;
    private String token;
    private boolean enabled;
    private String environment;
    private int feignConnectTimeout;
    private int feignReadTimeout;
}
