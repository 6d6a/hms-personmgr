package ru.majordomo.hms.personmgr.config;

import com.netshoes.springframework.cloud.sleuth.instrument.amqp.AmqpMessagingSpanExtractor;
import com.netshoes.springframework.cloud.sleuth.instrument.amqp.AmqpMessagingSpanInjector;
import com.netshoes.springframework.cloud.sleuth.instrument.amqp.AmqpMessagingSpanManager;
import com.netshoes.springframework.cloud.sleuth.instrument.amqp.AmqpTemplateAspect;
import com.netshoes.springframework.cloud.sleuth.instrument.amqp.DefaultAmqpMessagingSpanManager;
import com.netshoes.springframework.cloud.sleuth.instrument.amqp.RabbitListenerAspect;

import org.springframework.cloud.sleuth.TraceKeys;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Random;

@Configuration
public class SleuthConfig {
    @Bean
    public RabbitListenerAspect rabbitListenerAspect(
            AmqpMessagingSpanManager amqpMessagingSpanManager) {
        return new RabbitListenerAspect(amqpMessagingSpanManager);
    }

    @Bean
    public AmqpTemplateAspect amqpTemplateAspect(
            AmqpMessagingSpanManager amqpMessagingSpanManager) {
        return new AmqpTemplateAspect(amqpMessagingSpanManager);
    }

    @Bean
    public AmqpMessagingSpanManager amqpMessagingSpanManager(
            AmqpMessagingSpanInjector amqpMessagingSpanInjector,
            AmqpMessagingSpanExtractor amqpMessagingSpanExtractor,
            Tracer tracer) {
        return new DefaultAmqpMessagingSpanManager(
                amqpMessagingSpanInjector, amqpMessagingSpanExtractor, tracer);
    }

    @Bean
    public AmqpMessagingSpanInjector amqpMessagingSpanInjector(TraceKeys traceKeys) {
        return new AmqpMessagingSpanInjector(traceKeys);
    }

    @Bean
    public AmqpMessagingSpanExtractor amqpMessagingSpanExtractor(Random random) {
        return new AmqpMessagingSpanExtractor(random);
    }
}
