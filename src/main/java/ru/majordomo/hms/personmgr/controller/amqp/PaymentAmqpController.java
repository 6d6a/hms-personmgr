package ru.majordomo.hms.personmgr.controller.amqp;

import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.event.account.AccountPromotionProcessByPaymentCreatedEvent;
import ru.majordomo.hms.personmgr.event.account.AccountSwitchByPaymentCreatedEvent;
import ru.majordomo.hms.personmgr.model.PersonalAccount;

import java.util.HashMap;
import java.util.Map;

import static ru.majordomo.hms.personmgr.common.Constants.CREDIT_PAYMENT_TYPE_KIND;
import static ru.majordomo.hms.personmgr.common.Constants.REAL_PAYMENT_TYPE_KIND;

@EnableRabbit
@Service
public class PaymentAmqpController extends CommonAmqpController  {

    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(
                            value = "pm.payment.create",
                            durable = "true",
                            autoDelete = "false"
                    ),
                    exchange = @Exchange(
                            value = "payment.create",
                            type = ExchangeTypes.TOPIC
                    ),
                    key = "pm"
            )
    )
    public void create(@Payload SimpleServiceMessage message, @Headers Map<String, String> headers) {
        String provider = headers.get("provider");
        logger.debug("Received payment create message from " + provider + ": " + message.toString());

        PersonalAccount account = accountRepository.findOne(message.getAccountId());

        if (!message.getParam("paymentTypeKind").equals(CREDIT_PAYMENT_TYPE_KIND)) {

            if (account != null) {
                // P.S. У этого эвента делэй в 20 секунд
                publisher.publishEvent(new AccountSwitchByPaymentCreatedEvent(account));
            }

        }

        // Пополнение баланса партнера при поступлении средств клиенту, если этот клиент регистрировался по промокоду
        if (message.getParam("paymentTypeKind").equals(REAL_PAYMENT_TYPE_KIND)) {

            if (account != null) {
                Map<String, Object> paramsForPublisher = new HashMap<>();
                paramsForPublisher.put("amount", message.getParam("amount"));

                // P.S. У этого эвента делэй в 10 секунд
                publisher.publishEvent(new AccountPromotionProcessByPaymentCreatedEvent(account, paramsForPublisher));
            }
        }
    }
}