package ru.majordomo.hms.personmgr.controller.amqp;

import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import ru.majordomo.hms.personmgr.common.MailManagerMessageType;
import ru.majordomo.hms.personmgr.common.Utils;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.event.account.AccountPromotionProcessByPaymentCreatedEvent;
import ru.majordomo.hms.personmgr.event.account.AccountSwitchByPaymentCreatedEvent;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.service.AccountNotificationHelper;

import java.util.HashMap;
import java.util.Map;

import static ru.majordomo.hms.personmgr.common.Constants.CREDIT_PAYMENT_TYPE_KIND;
import static ru.majordomo.hms.personmgr.common.Constants.Exchanges.PAYMENT_CREATE;
import static ru.majordomo.hms.personmgr.common.Constants.REAL_PAYMENT_TYPE_KIND;

@Service
public class PaymentAmqpController extends CommonAmqpController  {

    private final AccountNotificationHelper accountNotificationHelper;

    @Autowired
    public PaymentAmqpController(
            AccountNotificationHelper accountNotificationHelper
    ) {
        this.accountNotificationHelper = accountNotificationHelper;
    }

    @RabbitListener(queues = "${spring.application.name}" + "." + PAYMENT_CREATE)
    public void create(@Payload SimpleServiceMessage message, @Headers Map<String, String> headers) {
        String provider = headers.get("provider");
        logger.debug("Received payment create message from " + provider + ": " + message.toString());

        PersonalAccount account = accountManager.findOne(message.getAccountId());

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

                try {
                    //Если подключено СМС-уведомление, то также отправим его
                    if (accountNotificationHelper.hasActiveSmsNotificationsAndMessageType(account, MailManagerMessageType.SMS_NEW_PAYMENT)) {

                        HashMap<String, String> paramsForSms = new HashMap<>();
                        paramsForSms.put("client_id", account.getAccountId());
                        paramsForSms.put("acc_id", account.getName());
                        paramsForSms.put("add_sum", Utils.formatBigDecimalWithCurrency(Utils.getBigDecimalFromUnexpectedInput(message.getParam("amount"))));

                        accountNotificationHelper.sendSms(account, "MajordomoHMSNewPayment", 10, paramsForSms);
                    }
                } catch (Exception e) {
                    logger.error("Exception at send sms in PaymentAmqpController.create .SMS for account " + account.getName() + " not send.");
                }
            }
        }
    }
}