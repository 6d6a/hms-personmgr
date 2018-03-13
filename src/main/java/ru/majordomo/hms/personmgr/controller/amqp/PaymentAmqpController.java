package ru.majordomo.hms.personmgr.controller.amqp;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.event.account.PaymentWasReceivedEvent;
import ru.majordomo.hms.personmgr.service.AccountNotificationHelper;

import java.util.Map;

import static ru.majordomo.hms.personmgr.common.Constants.Exchanges.PAYMENT_CREATE;

@Service
public class PaymentAmqpController extends CommonAmqpController  {

    private final AccountNotificationHelper accountNotificationHelper;

    @Autowired
    public PaymentAmqpController(
            AccountNotificationHelper accountNotificationHelper
    ) {
        this.accountNotificationHelper = accountNotificationHelper;
    }

    @RabbitListener(queues = "${hms.instance.name}" + "." + "${spring.application.name}" + "." + PAYMENT_CREATE)
    public void create(Message amqpMessage, @Payload SimpleServiceMessage message, @Headers Map<String, String> headers) {
        String provider = headers.get("provider");
        logger.debug("Received payment create message from " + provider + ": " + message.toString());

        publisher.publishEvent(new PaymentWasReceivedEvent(message));
    }
}