package ru.majordomo.hms.personmgr.controller.amqp;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.Map;

import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;

import static ru.majordomo.hms.personmgr.common.Constants.Exchanges.ACCOUNT_HISTORY;
import static ru.majordomo.hms.personmgr.common.Constants.HISTORY_MESSAGE_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.OPERATOR_KEY;

@Service
public class AccountHistoryAmqpController extends CommonAmqpController {

    @RabbitListener(queues = "${hms.instance.name}" + "." + "${spring.application.name}" + "." + ACCOUNT_HISTORY)
    public void create(Message amqpMessage, @Payload SimpleServiceMessage message, @Headers Map<String, String> headers) {
        String provider = headers.get("provider");
        logger.debug("Received from " + provider + ": " + message.toString());

        try {
            String historyMessage = (String) message.getParam(HISTORY_MESSAGE_KEY);
            String operator = (String) message.getParam(OPERATOR_KEY);

            if (historyMessage != null && operator != null) {
                history.addMessage(message.getAccountId(), historyMessage, operator);
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Got Exception in AccountHistoryAmqpController.create " + e.getMessage());
        }
    }
}
