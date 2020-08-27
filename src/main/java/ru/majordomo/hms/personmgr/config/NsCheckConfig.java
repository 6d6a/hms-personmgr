package ru.majordomo.hms.personmgr.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Set;

@Data
@Component
@ConfigurationProperties(prefix = "ns-check")
public class NsCheckConfig {
    private String dnsResolverIp;
    /** список разрешенных NS записей без точек в конце */
    private Set<String> allowedNsList;
}
