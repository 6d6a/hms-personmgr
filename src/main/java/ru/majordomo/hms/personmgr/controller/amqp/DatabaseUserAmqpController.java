package ru.majordomo.hms.personmgr.controller.amqp;

import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

import ru.majordomo.hms.personmgr.common.State;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.event.accountHistory.AccountHistoryEvent;

import static ru.majordomo.hms.personmgr.common.Constants.HISTORY_MESSAGE_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.OPERATOR_KEY;

@EnableRabbit
@Service
public class DatabaseUserAmqpController extends CommonAmqpController {
    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(
                            value = "pm.database-user.create",
                            durable = "true",
                            autoDelete = "false"
                    ),
                    exchange = @Exchange(
                            value = "database-user.create",
                            type = ExchangeTypes.TOPIC
                    ),
                    key = "pm"
            )
    )
    public void create(@Payload SimpleServiceMessage message, @Headers Map<String, String> headers) {
        String provider = headers.get("provider");
        logger.debug("Received from " + provider + ": " + message.toString());

        State state = businessFlowDirector.processMessage(message);

        if (state.equals(State.PROCESSED)) {
            //Save history
            Map<String, String> params = new HashMap<>();
            params.put(HISTORY_MESSAGE_KEY, "Заявка на создание пользователя баз данных выполнена успешно (имя: " + message.getParam("name") + ")");
            params.put(OPERATOR_KEY, "service");

            publisher.publishEvent(new AccountHistoryEvent(message.getAccountId(), params));
        }
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "pm.database-user.update",
                           durable = "true",
                           autoDelete = "false"),
            exchange = @Exchange(value = "database-user.update",
                                 type = ExchangeTypes.TOPIC),
            key = "pm"))
    public void update(@Payload SimpleServiceMessage message, @Headers Map<String, String> headers) {
        String provider = headers.get("provider");
        logger.debug("Received update message from " + provider + ": " + message.toString());

        State state = businessFlowDirector.processMessage(message);



        if (state.equals(State.PROCESSED)) {
            //Save history
            Map<String, String> params = new HashMap<>();
            params.put(HISTORY_MESSAGE_KEY, "Заявка на обновление пользователя баз данных выполнена успешно (имя: " + message.getParam("name") + ")");
            params.put(OPERATOR_KEY, "service");

            publisher.publishEvent(new AccountHistoryEvent(message.getAccountId(), params));
        }
    }

    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = "pm.database-user.delete",
                                                            durable = "true",
                                                            autoDelete = "false"),
                                             exchange = @Exchange(value = "database-user.delete",
                                                                  type = ExchangeTypes.TOPIC),
                                             key = "pm"))
    public void delete(@Payload SimpleServiceMessage message, @Headers Map<String, String> headers) {
        String provider = headers.get("provider");
        logger.debug("Received delete message from " + provider + ": " + message.toString());

        State state = businessFlowDirector.processMessage(message);

        if (state.equals(State.PROCESSED)) {
            //Save history
            Map<String, String> params = new HashMap<>();
            params.put(HISTORY_MESSAGE_KEY, "Заявка на удаление пользователя баз данных выполнена успешно (имя: " + message.getParam("name") + ")");
            params.put(OPERATOR_KEY, "service");

            publisher.publishEvent(new AccountHistoryEvent(message.getAccountId(), params));
        }
    }
}
