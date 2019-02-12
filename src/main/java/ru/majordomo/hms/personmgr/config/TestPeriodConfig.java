package ru.majordomo.hms.personmgr.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "test-period")
@Data
@Component
public class TestPeriodConfig {
    private List<String> disallowDomainZones = new ArrayList<>();
}
