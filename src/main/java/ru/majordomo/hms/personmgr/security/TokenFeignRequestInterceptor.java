package ru.majordomo.hms.personmgr.security;

import feign.RequestInterceptor;
import feign.RequestTemplate;

public class TokenFeignRequestInterceptor implements RequestInterceptor {
    private final String headerValue;

    public TokenFeignRequestInterceptor(String token) {
        this.headerValue = "Token " + token;
    }

    @Override
    public void apply(RequestTemplate template) {
        template.header("Authorization", headerValue);
    }
}
