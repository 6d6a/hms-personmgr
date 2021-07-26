package ru.majordomo.hms.personmgr;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.module.SimpleModule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.PageImpl;
import org.springframework.retry.annotation.EnableRetry;

import ru.majordomo.hms.personmgr.config.AlertaSettings;
import ru.majordomo.hms.personmgr.config.FeignConfig;
import ru.majordomo.hms.personmgr.config.HikariSettings;
import ru.majordomo.hms.personmgr.config.HmsProperties;
import ru.majordomo.hms.personmgr.serializer.PageSerializer;

@SpringBootApplication(exclude = {
        DataSourceAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class
})
@EnableDiscoveryClient
@EnableRetry
@EnableConfigurationProperties({HikariSettings.class, AlertaSettings.class, HmsProperties.class})
@EnableFeignClients(basePackages = "ru.majordomo.hms.personmgr.feign")
public class Application implements CommandLineRunner {
    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    public void run(String... args) {
        StringBuilder sb = new StringBuilder();
        for (String option : args) {
            sb.append(" ").append(option);
        }
        sb = sb.length() == 0 ? sb.append("No Options Specified") : sb;
        logger.info(String.format("Launched personal manager with following options: %s", sb.toString()));
    }

    @Bean
    public Module jacksonPageWithJsonViewModule() {
        SimpleModule module = new SimpleModule("jackson-page-with-jsonview", Version.unknownVersion());
        module.addSerializer(PageImpl.class, new PageSerializer());
        return module;
    }
}
