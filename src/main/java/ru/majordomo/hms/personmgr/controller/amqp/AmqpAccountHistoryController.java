package ru.majordomo.hms.personmgr.controller.amqp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.service.AccountHistoryService;

import java.util.Map;

@EnableRabbit
@Service
public class AmqpAccountHistoryController {

    private final static Logger logger = LoggerFactory.getLogger(AmqpAccountHistoryController.class);

    private final AccountHistoryService accountHistoryService;

    @Autowired
    public AmqpAccountHistoryController(AccountHistoryService accountHistoryService) {
        this.accountHistoryService = accountHistoryService;
    }

    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = "pm.account-history", durable = "true", autoDelete = "true"), exchange = @Exchange(value = "account-history", type = ExchangeTypes.TOPIC), key = "pm"))
    public void create(@Payload SimpleServiceMessage message, @Headers Map<String, String> headers) {
        String provider = headers.get("provider");
        logger.debug("Received from " + provider + ": " + message.toString());

        String historyMessage = (String) message.getParam("historyMessage");
        String operator = (String) message.getParam("operator");

        if (historyMessage != null && operator != null) {
            accountHistoryService.addMessage(message.getAccountId(), historyMessage, operator);
        }
    }
}
