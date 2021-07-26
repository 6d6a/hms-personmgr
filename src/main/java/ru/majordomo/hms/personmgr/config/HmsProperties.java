package ru.majordomo.hms.personmgr.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "hms")
public class HmsProperties {
    /** адрес frontend биллинга, без / в конце */
    private String billingUrl;

    private String apiUrl;

    private Instance instance;

    @Data
    public static class Instance {
        private String name;
    }
}
