package ru.majordomo.hms.personmgr.config;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.AsyncConfigurerSupport;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

import ru.majordomo.hms.personmgr.exception.handler.MyAsyncUncaughtExceptionHandler;

@Configuration
@EnableAsync
public class AsyncConfig extends AsyncConfigurerSupport {
    @Bean(name = "threadPoolTaskExecutor")
    @Primary
    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(6);
        executor.setMaxPoolSize(6);
        executor.setThreadNamePrefix("PM-Thread-");
        executor.initialize();
        return executor;
    }

    @Bean(name = "vipThreadPoolTaskExecutor")
    public Executor getVipAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(3);
        executor.setThreadNamePrefix("PM-Vip-");
        executor.initialize();
        return executor;
    }

    @Bean(name = "quotaThreadPoolTaskExecutor")
    @Qualifier("quotaThread")
    public Executor getQuotaAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(6);
        executor.setMaxPoolSize(6);
        executor.setThreadNamePrefix("PM-Quota-");
        executor.initialize();
        return executor;
    }

    @Bean(name = "revisiumThreadPoolTaskExecutor")
    public Executor getRevisiumThreadPoolTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(3);
        executor.setThreadNamePrefix("PM-Revisium-");
        executor.initialize();
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new MyAsyncUncaughtExceptionHandler();
    }
}
