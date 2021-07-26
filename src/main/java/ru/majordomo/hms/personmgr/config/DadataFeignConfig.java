package ru.majordomo.hms.personmgr.config;

import feign.Request;
import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import ru.majordomo.hms.personmgr.security.TokenFeignRequestInterceptor;


public class DadataFeignConfig {
    @Value("${dadata.feign.connectTimeout:7000}")
    private int connectTimeout;

    @Value("${dadata.feign.readTimeOut:7000}")
    private int readTimeout;

    @Value("${dadata.token}")
    private String token;

    @Bean
    public RequestInterceptor tokenAuthRequestInterceptor() {
        return new TokenFeignRequestInterceptor(token);
    }

    @Bean
    public Request.Options options() {
        return new Request.Options(connectTimeout, readTimeout);
    }
}
