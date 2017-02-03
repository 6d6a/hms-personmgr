package ru.majordomo.hms.personmgr.controller.amqp;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.type.TypeFactory;
import org.codehaus.jackson.type.JavaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

import ru.majordomo.hms.personmgr.common.BusinessActionType;
import ru.majordomo.hms.personmgr.common.State;
import ru.majordomo.hms.personmgr.common.message.MailManagerEmailMessage;
import ru.majordomo.hms.personmgr.common.message.MailManagerEmailMessageParams;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.service.BusinessActionBuilder;
import ru.majordomo.hms.personmgr.service.BusinessFlowDirector;

@EnableRabbit
@Service
public class WebSiteAmqpController {

    private final static Logger logger = LoggerFactory.getLogger(WebSiteAmqpController.class);

    private final BusinessActionBuilder businessActionBuilder;
    private final BusinessFlowDirector businessFlowDirector;

    @Value( "${mail_manager.dev_email}" )
    private String mailManagerDevEmail;

    @Autowired
    public WebSiteAmqpController(
            BusinessActionBuilder businessActionBuilder,
            BusinessFlowDirector businessFlowDirector
    ) {
        this.businessActionBuilder = businessActionBuilder;
        this.businessFlowDirector = businessFlowDirector;
    }

    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(
                            value = "pm.website.create",
                            durable = "true",
                            autoDelete = "false"
                    ),
                    exchange = @Exchange(
                            value = "website.create",
                            type = ExchangeTypes.TOPIC
                    ),
                    key = "pm"
            )
    )
    public void create(@Payload SimpleServiceMessage message, @Headers Map<String, String> headers) {
        String provider = headers.get("provider");
        logger.debug("Received create message from " + provider + ": " + message.toString());

        State state = businessFlowDirector.processMessage(message);

        if (state == State.PROCESSED) {
            MailManagerEmailMessage mailManagerEmailMessage = new MailManagerEmailMessage();
            mailManagerEmailMessage.setOperationIdentity(message.getOperationIdentity());
            MailManagerEmailMessageParams messageParams = new MailManagerEmailMessageParams();
            messageParams.setEmail(mailManagerDevEmail);
            messageParams.setApi_name("MajordomoVHWebSiteCreated");
            messageParams.setPriority(10);

            HashMap<String, String> parameters = new HashMap<>();
            parameters.put("client_id", "ac_100800");
            parameters.put("website_name", "test-site.ru");

            messageParams.setParametrs(parameters);
            mailManagerEmailMessage.setParams(messageParams);

            ObjectMapper mapper = new ObjectMapper();
            TypeFactory typeFactory = mapper.getTypeFactory();
            JavaType mapType = typeFactory.constructType(SimpleServiceMessage.class);

            SimpleServiceMessage serviceMessage = mapper.convertValue(mailManagerEmailMessage, mapType);

            businessActionBuilder.build(BusinessActionType.WEB_SITE_CREATE_MM, serviceMessage);
        }
    }

    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(
                            value = "pm.website.update",
                            durable = "true",
                            autoDelete = "false"
                    ),
                    exchange = @Exchange(
                            value = "website.update",
                            type = ExchangeTypes.TOPIC
                    ),
                    key = "pm"
            )
    )
    public void update(@Payload SimpleServiceMessage message, @Headers Map<String, String> headers) {
        String provider = headers.get("provider");
        logger.debug("Received update message from " + provider + ": " + message.toString());

        State state = businessFlowDirector.processMessage(message);
    }

    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(
                            value = "pm.website.delete",
                            durable = "true",
                            autoDelete = "false"
                    ),
                    exchange = @Exchange(
                            value = "website.delete",
                            type = ExchangeTypes.TOPIC
                    ),
                    key = "pm"
            )
    )
    public void delete(@Payload SimpleServiceMessage message, @Headers Map<String, String> headers) {
        String provider = headers.get("provider");
        logger.debug("Received delete message from " + provider + ": " + message.toString());

        State state = businessFlowDirector.processMessage(message);
    }
}
