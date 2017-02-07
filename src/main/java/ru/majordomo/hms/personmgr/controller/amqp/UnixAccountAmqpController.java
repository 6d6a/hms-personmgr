package ru.majordomo.hms.personmgr.controller.amqp;

import com.netflix.servo.util.Strings;

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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.majordomo.hms.personmgr.common.BusinessActionType;
import ru.majordomo.hms.personmgr.common.BusinessOperationType;
import ru.majordomo.hms.personmgr.common.State;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.model.ProcessingBusinessOperation;
import ru.majordomo.hms.personmgr.repository.ProcessingBusinessOperationRepository;
import ru.majordomo.hms.personmgr.service.BusinessActionBuilder;
import ru.majordomo.hms.personmgr.service.BusinessFlowDirector;

@EnableRabbit
@Service
public class UnixAccountAmqpController {

    private final static Logger logger = LoggerFactory.getLogger(UnixAccountAmqpController.class);

    private final BusinessFlowDirector businessFlowDirector;
    private final ProcessingBusinessOperationRepository processingBusinessOperationRepository;
    private final BusinessActionBuilder businessActionBuilder;

    @Autowired
    public UnixAccountAmqpController(
            BusinessFlowDirector businessFlowDirector,
            ProcessingBusinessOperationRepository processingBusinessOperationRepository,
            BusinessActionBuilder businessActionBuilder
    ) {
        this.businessFlowDirector = businessFlowDirector;
        this.processingBusinessOperationRepository = processingBusinessOperationRepository;
        this.businessActionBuilder = businessActionBuilder;
    }

    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(
                            value = "pm.unix-account.create",
                            durable = "true",
                            autoDelete = "false"
                    ),
                    exchange = @Exchange(
                            value = "unix-account.create",
                            type = ExchangeTypes.TOPIC
                    ),
                    key = "pm"
            )
    )
    @SuppressWarnings("unchecked")
    public void create(@Payload SimpleServiceMessage message, @Headers Map<String, String> headers) {
        String provider = headers.get("provider");
        logger.debug("Received from " + provider + ": " + message.toString());

        State state = businessFlowDirector.processMessage(message);

        if (state == State.PROCESSED) {
            ProcessingBusinessOperation businessOperation = processingBusinessOperationRepository.findOne(message.getOperationIdentity());
            if (businessOperation != null && businessOperation.getType() == BusinessOperationType.ACCOUNT_CREATE) {
                businessOperation.setState(State.PROCESSED);
                processingBusinessOperationRepository.save(businessOperation);

                List<String> emails = (List<String>) businessOperation.getMapParam("emailAddresses");
                message.setParams(new HashMap<>());
                message.addParam("email", Strings.join(",", emails.iterator()));
                message.addParam("api_name", "MajordomoVHClientCreatedConfirmation");
                message.addParam("priority", 10);

                HashMap<String, String> parameters = new HashMap<>();
                parameters.put("client_id", message.getAccountId());
                parameters.put("password", (String) businessOperation.getMapParam("password"));
                parameters.put("ftp_ip", "ftp_ip");
                parameters.put("ftp_login", "ftp_login");
                parameters.put("ftp_password", "ftp_password");

                message.addParam("parametrs", parameters);

                businessActionBuilder.build(BusinessActionType.ACCOUNT_CREATE_MM, message);
            }
        }
    }

    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(
                            value = "pm.unix-account.update",
                            durable = "true",
                            autoDelete = "false"
                    ),
                    exchange = @Exchange(
                            value = "unix-account.update",
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
                            value = "pm.unix-account.delete",
                            durable = "true",
                            autoDelete = "false"
                    ),
                    exchange = @Exchange(
                            value = "unix-account.delete",
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