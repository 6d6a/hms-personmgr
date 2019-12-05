package ru.majordomo.hms.personmgr.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.retry.interceptor.RetryOperationsInterceptor;

import java.util.ArrayList;
import java.util.List;

import static ru.majordomo.hms.personmgr.common.Constants.Exchanges.ALL_EXCHANGES;

@Configuration
@EnableRabbit
public class RabbitMqConfig {

    @Value("${spring.rabbitmq.host}")
    private String rabbitHost;

    @Value("${spring.rabbitmq.username}")
    private String rabbitUsername;

    @Value("${spring.rabbitmq.password}")
    private String rabbitPassword;


    @Value("${spring.application.name}")
    private String applicationName;

    @Value("${hms.instance.name}")
    private String instanceName;

    @Bean
    public ConnectionFactory connectionFactory() {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory(rabbitHost);
        connectionFactory.setUsername(rabbitUsername);
        connectionFactory.setPassword(rabbitPassword);
        return connectionFactory;
    }

    @Bean
    public AmqpAdmin amqpAdmin() {
        return new RabbitAdmin(connectionFactory());
    }

    @Bean
    @Primary
    public RabbitTemplate rabbitTemplate() {
        return new RabbitTemplate(connectionFactory());
    }

    @Bean
    public Jackson2JsonMessageConverter converter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    RetryOperationsInterceptor interceptor() {
        return RetryInterceptorBuilder.stateless()
                .maxAttempts(3)
                .recoverer(
                        new RepublishMessageRecoverer(
                                rabbitTemplate(),
                                instanceName + "." + applicationName,
                                "error"
                        )
                )
                .build();
    }

    @Bean
    public Declarables exchanges() {
        List<Declarable> exchanges = new ArrayList<>();

        for (String exchangeName : ALL_EXCHANGES) {
            exchanges.add(new TopicExchange(exchangeName));
        }

        return new Declarables(exchanges);
    }

    @Bean
    public Declarables queues() {
        List<Declarable> queues = new ArrayList<>();

        for (String exchangeName : ALL_EXCHANGES) {
            queues.add(new Queue(instanceName + "." + applicationName + "." + exchangeName));
        }

        return new Declarables(queues);
    }

    @Bean
    public Declarables bindings() {
        List<Declarable> bindings = new ArrayList<>();

        for (String exchangeName : ALL_EXCHANGES) {
            bindings.add(new Binding(
                    instanceName + "." + applicationName + "." + exchangeName,
                    Binding.DestinationType.QUEUE,
                    exchangeName,
                    instanceName + "." + applicationName,
                    null
            ));
        }

        return new Declarables(bindings);
    }
}