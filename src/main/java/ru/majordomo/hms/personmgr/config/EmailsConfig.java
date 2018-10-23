package ru.majordomo.hms.personmgr.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import ru.majordomo.hms.personmgr.common.Department;

import java.util.Map;

@ConfigurationProperties(prefix = "emails")
@Component
@Data
public class EmailsConfig {
    private Map<Department, String> departments;
}
