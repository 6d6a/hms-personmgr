package ru.majordomo.hms.personmgr.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "mail-manager")
@Component
@Data
public class MailManagerConfig {
    private String username;
    private String password;
    private String url;
}
