package ru.majordomo.hms.personmgr.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurerAdapter;

import ru.majordomo.hms.personmgr.model.service.PaymentService;

/**
 * RepositoryRestConfiguration
 */
@Configuration
public class RepositoryRestConfig extends RepositoryRestConfigurerAdapter {
    @Override
    public void configureRepositoryRestConfiguration(RepositoryRestConfiguration config) {
        config.exposeIdsFor(PaymentService.class);
        super.configureRepositoryRestConfiguration(config);
    }
}
