package ru.majordomo.hms.personmgr.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotNull;

@Data
@ConfigurationProperties("hikari")
@Validated
public class HikariSettings {
    @NotNull
    private int connectionTimeout;
    @NotNull
    private int idleTimeout;
    @NotNull
    private int maximumPoolSize;
    @NotNull
    private int maxLifetime;
}
