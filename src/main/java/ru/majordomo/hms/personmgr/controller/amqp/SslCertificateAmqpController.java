package ru.majordomo.hms.personmgr.controller.amqp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.Map;

import ru.majordomo.hms.personmgr.common.State;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.service.BusinessFlowDirector;

@EnableRabbit
@Service
public class SslCertificateAmqpController {

    private final static Logger logger = LoggerFactory.getLogger(SslCertificateAmqpController.class);

    private final BusinessFlowDirector businessFlowDirector;

    @Autowired
    public SslCertificateAmqpController(BusinessFlowDirector businessFlowDirector) {
        this.businessFlowDirector = businessFlowDirector;
    }

    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = "pm.ssl-certificate.create",
                                                            durable = "true",
                                                            autoDelete = "false"),
                                             exchange = @Exchange(value = "ssl-certificate.create",
                                                                  type = ExchangeTypes.TOPIC),
                                             key = "pm"))
    public void create(@Payload SimpleServiceMessage message, @Headers Map<String, String> headers) {
        String provider = headers.get("provider");
        logger.debug("Received from " + provider + ": " + message.toString());

        businessFlowDirector.processMessage(message);
    }

    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = "pm.ssl-certificate.update",
                                                            durable = "true",
                                                            autoDelete = "false"),
                                             exchange = @Exchange(value = "ssl-certificate.update",
                                                                  type = ExchangeTypes.TOPIC),
                                             key = "pm"))
    public void update(@Payload SimpleServiceMessage message, @Headers Map<String, String> headers) {
        String provider = headers.get("provider");
        logger.debug("Received update message from " + provider + ": " + message.toString());

        State state = businessFlowDirector.processMessage(message);
    }

    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = "pm.ssl-certificate.delete",
                                                            durable = "true",
                                                            autoDelete = "false"),
                                             exchange = @Exchange(value = "ssl-certificate.delete",
                                                                  type = ExchangeTypes.TOPIC),
                                             key = "pm"))
    public void delete(@Payload SimpleServiceMessage message, @Headers Map<String, String> headers) {
        String provider = headers.get("provider");
        logger.debug("Received delete message from " + provider + ": " + message.toString());

        State state = businessFlowDirector.processMessage(message);
    }
}
