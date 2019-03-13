package ru.majordomo.hms.personmgr.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotBlank;

@ConfigurationProperties(prefix = "gogetssl")
@Component
@Data
public class GoGetSSLConfig {
    @NotBlank
    private String apiUrl;
    @NotBlank
    private String login;
    @NotBlank
    private String password;
}
