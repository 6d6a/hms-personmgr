package ru.majordomo.hms.personmgr.config;

import com.mongodb.MongoClient;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.mongo.MongoLockProvider;
import net.javacrumbs.shedlock.spring.SpringLockableTaskSchedulerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class SchedulingConfig {
    private final MongoClient mongoClient;

    @Value("${spring.data.mongodb.database}")
    private String mongodbDatabaseName;

    @Autowired
    public SchedulingConfig(
            MongoClient mongo
    ) {
        this.mongoClient = mongo;
    }

    @Bean
    public LockProvider lockProvider() throws Exception {
        return new MongoLockProvider(mongoClient, mongodbDatabaseName);
    }

    @Bean
    public TaskScheduler taskScheduler(LockProvider lockProvider) {
        int poolSize = 8;
        return SpringLockableTaskSchedulerFactory.newLockableTaskScheduler(poolSize, lockProvider);
    }
}
