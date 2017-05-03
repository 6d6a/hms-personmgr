package ru.majordomo.hms.personmgr;

import com.mongodb.Mongo;
import com.mongodb.MongoClient;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.mongo.MongoLockProvider;
import net.javacrumbs.shedlock.spring.SpringLockableTaskSchedulerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import java.time.Duration;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Configuration
@EnableScheduling
public class SchedulingConfig implements SchedulingConfigurer {
    @Autowired MongoConfig mongoConfig;

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.setScheduler(taskExecutor());
    }

    @Bean(name = "scheduledTaskExecutor", destroyMethod = "shutdown")
    public ScheduledExecutorService taskExecutor() {
        return Executors.newScheduledThreadPool(8);
    }

    @Bean
    public LockProvider lockProvider() throws Exception {
        return new MongoLockProvider(mongoConfig.mongo(), "synchronized");
    }

    @Bean
    public TaskScheduler taskScheduler(ScheduledExecutorService executorService, LockProvider lockProvider) {
        return SpringLockableTaskSchedulerFactory.newLockableTaskScheduler(executorService, lockProvider, Duration.ofMinutes(27));
    }
}
