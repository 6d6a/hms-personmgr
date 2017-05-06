package ru.majordomo.hms.personmgr.controller.amqp;

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

import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.service.AccountHistoryService;

import static ru.majordomo.hms.personmgr.common.Constants.HISTORY_MESSAGE_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.OPERATOR_KEY;

@EnableRabbit
@Service
public class AccountHistoryAmqpController extends CommonAmqpController {
    private final AccountHistoryService accountHistoryService;

    @Autowired
    public AccountHistoryAmqpController(AccountHistoryService accountHistoryService) {
        this.accountHistoryService = accountHistoryService;
    }

    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = "pm.account-history",
                                                            durable = "true",
                                                            autoDelete = "false"),
                                             exchange = @Exchange(value = "account-history",
                                                                  type = ExchangeTypes.TOPIC),
                                             key = "pm"))
    public void create(@Payload SimpleServiceMessage message, @Headers Map<String, String> headers) {
        String provider = headers.get("provider");
        logger.debug("Received from " + provider + ": " + message.toString());

        try {
            String historyMessage = (String) message.getParam(HISTORY_MESSAGE_KEY);
            String operator = (String) message.getParam(OPERATOR_KEY);

            if (historyMessage != null && operator != null) {
                accountHistoryService.addMessage(message.getAccountId(), historyMessage, operator);
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Got Exception in ru.majordomo.hms.personmgr.controller.amqp.AccountHistoryAmqpController.create " + e.getMessage());
        }
    }
}
