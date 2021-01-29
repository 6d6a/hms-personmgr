package ru.majordomo.hms.personmgr.config;

import feign.Logger;
import feign.Request;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

public class WkHttpToPdfFeignConfig {
    @Value("${service.feign.connectTimeout:30000}")
    private int connectTimeout;

    @Value("${service.feign.readTimeOut:120000}")
    private int readTimeout;

    @Bean
    Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }

    @Bean
    public Request.Options options() {
        return new Request.Options(connectTimeout, readTimeout);
    }
}
