package ru.majordomo.hms.personmgr.config;

import feign.Request;
import feign.RequestInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import ru.majordomo.hms.personmgr.security.TokenFeignRequestInterceptor;


@RequiredArgsConstructor
public class AlertaFeignConfig {
    private final AlertaSettings alertaSettings;

    @Bean
    public RequestInterceptor tokenAuthRequestInterceptor() {
        return new TokenFeignRequestInterceptor(alertaSettings.getToken(), "Key ");
    }

    @Bean
    public Request.Options options() {
        return new Request.Options(alertaSettings.getFeignConnectTimeout(), alertaSettings.getFeignReadTimeout());
    }
}
