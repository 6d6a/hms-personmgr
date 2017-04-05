package ru.majordomo.hms.personmgr;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.DefaultOAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.client.token.AccessTokenRequest;
import org.springframework.security.oauth2.client.token.DefaultAccessTokenRequest;
import org.springframework.security.oauth2.client.token.grant.password.ResourceOwnerPasswordResourceDetails;
import org.springframework.web.context.request.RequestContextListener;

import java.util.Collections;

import feign.RequestInterceptor;
import ru.majordomo.hms.personmgr.security.OAuth2FeignRequestInterceptor;

@Configuration
public class FeignConfig {
    @Value("${security.oauth2.client.accessTokenUri}")
    private String accessTokenUri;

    @Value("${security.oauth2.client.clientId}")
    private String clientId;

    @Value("${security.oauth2.client.clientSecret}")
    private String clientSecret;

    @Value("${security.oauth2.client.scope}")
    private String scope;

    @Value("${si_oauth.serviceUsername}")
    private String username;

    @Value("${si_oauth.servicePassword}")
    private String password;

    @Bean
    public RequestInterceptor oauth2FeignRequestInterceptor(){
        AccessTokenRequest accessTokenRequest = new DefaultAccessTokenRequest();
        OAuth2ClientContext oAuth2ClientContext = new DefaultOAuth2ClientContext(accessTokenRequest);

        return new OAuth2FeignRequestInterceptor(oAuth2ClientContext, resource());
    }

    private OAuth2ProtectedResourceDetails resource() {
        ResourceOwnerPasswordResourceDetails details = new ResourceOwnerPasswordResourceDetails();
        details.setAccessTokenUri(accessTokenUri);
        details.setClientId(clientId);
        details.setClientSecret(clientSecret);
        details.setScope(Collections.singletonList(scope));
        details.setGrantType("password");
        details.setUsername(username);
        details.setPassword(password);

        return details;
    }

    @Bean public RequestContextListener requestContextListener(){
        return new RequestContextListener();
    }
}
