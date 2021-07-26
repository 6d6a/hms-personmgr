package ru.majordomo.hms.personmgr.security;

import feign.RequestInterceptor;
import feign.RequestTemplate;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class TokenFeignRequestInterceptor implements RequestInterceptor {
    private final String headerValue;

    public TokenFeignRequestInterceptor(String token) {
        this.headerValue = "Token " + token;
    }

    public TokenFeignRequestInterceptor(String token, String tokenName) {
        this.headerValue = tokenName + token;
    }

    @Override
    public void apply(RequestTemplate template) {
        template.header("Authorization", headerValue);
    }
}
