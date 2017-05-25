package ru.majordomo.hms.personmgr.config;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.mongo.MongoLockProvider;
import net.javacrumbs.shedlock.spring.SpringLockableTaskSchedulerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class SchedulingConfig {
    @Autowired MongoConfig mongoConfig;

    @Bean
    public LockProvider lockProvider() throws Exception {
        return new MongoLockProvider(mongoConfig.mongo(), mongoConfig.getDatabaseName());
    }

    @Bean
    public TaskScheduler taskScheduler(LockProvider lockProvider) {
        int poolSize = 8;
        return SpringLockableTaskSchedulerFactory.newLockableTaskScheduler(poolSize, lockProvider);
    }
}
