package ru.majordomo.hms.personmgr.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

@Configuration
@EnableCaching
public class CacheConfig extends CachingConfigurerSupport {
    @Bean
    @Override
    public CacheManager cacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(Arrays.asList(
                new ConcurrentMapCache("paymentServices"),
                new ConcurrentMapCache("paymentServicesActive"),
                new ConcurrentMapCache("paymentServicesOldId"),
                new ConcurrentMapCache("plans"),
                new ConcurrentMapCache("promocodes"),
                new ConcurrentMapCache("servicePlans"),
                new ConcurrentMapCache("plansById"),
                new ConcurrentMapCache("plansByActive"),
                new ConcurrentMapCache("plansByName"),
                new ConcurrentMapCache("plansByAccountType"),
                new ConcurrentMapCache("plansByServiceId"),
                new ConcurrentMapCache("plansByOldId"),
                new ConcurrentMapCache("servicePlansById"),
                new ConcurrentMapCache("servicePlansByFeature"),
                new ConcurrentMapCache("servicePlansByActive"),
                new ConcurrentMapCache("servicePlansByFeatureAndActive"),
                new ConcurrentMapCache("servicePlansByFeatureAndServiceId")
        ));

        return cacheManager;
    }
}
