package ru.majordomo.hms.personmgr.config;

import com.google.common.cache.CacheBuilder;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

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
                new ConcurrentMapCache("plansByAbonementIds"),
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
                new ConcurrentMapCache("servicePlansByFeatureAndServiceId"),
                new ConcurrentMapCache("getTicketWithMessages",
                        CacheBuilder.newBuilder().expireAfterWrite(5, TimeUnit.MINUTES).maximumSize(1000).build().asMap(),
                        false
                ),
                new ConcurrentMapCache("countryById"),
                new ConcurrentMapCache("countryFindAll"),
                new ConcurrentMapCache("sslServerTypeById"),
                new ConcurrentMapCache("sslServerTypeFindAll"),
                new ConcurrentMapCache("approverEmailFindAll"),
                new ConcurrentMapCache("approverEmailById"),
                new ConcurrentMapCache("sslSupplierById"),
                new ConcurrentMapCache("sslSupplierFindAll"),
                new ConcurrentMapCache("sslProductFindAll"),
                new ConcurrentMapCache("sslProductById")
        ));

        return cacheManager;
    }
}
