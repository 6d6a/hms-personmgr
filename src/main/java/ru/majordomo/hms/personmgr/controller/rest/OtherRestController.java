package ru.majordomo.hms.personmgr.controller.rest;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang.SystemUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringBootVersion;
import org.springframework.context.ApplicationContext;
import org.springframework.core.SpringVersion;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.majordomo.hms.personmgr.config.HmsProperties;
import ru.majordomo.hms.personmgr.dto.other.ImportantVariables;

import java.util.Arrays;

/**
 * Контроллер нужен преимущественно для диагностики. Например к какому стеку и с какими настройками происходит обращение
 */
@RestController
@RequestMapping("/other")
@RequiredArgsConstructor
public class OtherRestController {
    @Value("${spring.application.name}") private final String applicationName;
    private final HmsProperties hmsProperties;
    private final Environment env;
    private final ApplicationContext applicationContext;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping(value = "/variables")
    public ResponseEntity<ImportantVariables> getImportantVariables() {
        ImportantVariables importantVariables = new ImportantVariables();
        importantVariables.setInstanceName(hmsProperties.getInstance().getName());
        importantVariables.setApplicationName(applicationName);
        importantVariables.setActiveProfile(Arrays.asList(env.getActiveProfiles()));
        importantVariables.setSpringBootVersion(SpringBootVersion.getVersion());
        importantVariables.setSpringVersion(SpringVersion.getVersion());
        importantVariables.setApplicationContextId(applicationContext.getId());
        importantVariables.setJavaVersion(SystemUtils.JAVA_VERSION);
        return ResponseEntity.ok(importantVariables);
    }
}
