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
public class FtpUserAmqpController extends CommonAmqpController {
    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = "pm.ftp-user.create",
                                                            durable = "true",
                                                            autoDelete = "false"),
                                             exchange = @Exchange(value = "ftp-user.create",
                                                                  type = ExchangeTypes.TOPIC),
                                             key = "pm"))
    public void create(@Payload SimpleServiceMessage message, @Headers Map<String, String> headers) {
        String provider = headers.get("provider");
        logger.debug("Received from " + provider + ": " + message.toString());

        try {
            State state = businessFlowDirector.processMessage(message);

            if (state.equals(State.PROCESSED)) {
                //Save history
                Map<String, String> params = new HashMap<>();
                params.put(HISTORY_MESSAGE_KEY, "Заявка на создание FTP-пользователя выполнена успешно (имя: " + message.getParam("name") + ")");
                params.put(OPERATOR_KEY, "service");

                publisher.publishEvent(new AccountHistoryEvent(message.getAccountId(), params));
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Got Exception in ru.majordomo.hms.personmgr.controller.amqp.FtpUserAmqpController.create " + e.getMessage());
        }
    }

    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = "pm.ftp-user.update",
                                                            durable = "true",
                                                            autoDelete = "false"),
                                             exchange = @Exchange(value = "ftp-user.update",
                                                                  type = ExchangeTypes.TOPIC),
                                             key = "pm"))
    public void update(@Payload SimpleServiceMessage message, @Headers Map<String, String> headers) {
        String provider = headers.get("provider");
        logger.debug("Received update message from " + provider + ": " + message.toString());

        try {
            State state = businessFlowDirector.processMessage(message);

            if (state.equals(State.PROCESSED)) {
                //Save history
                Map<String, String> params = new HashMap<>();
                params.put(HISTORY_MESSAGE_KEY, "Заявка на обновление FTP-пользователя выполнена успешно (имя: " + message.getParam("name") + ")");
                params.put(OPERATOR_KEY, "service");

                publisher.publishEvent(new AccountHistoryEvent(message.getAccountId(), params));
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Got Exception in ru.majordomo.hms.personmgr.controller.amqp.FtpUserAmqpController.update " + e.getMessage());
        }
    }

    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = "pm.ftp-user.delete",
                                                            durable = "true",
                                                            autoDelete = "false"),
                                             exchange = @Exchange(value = "ftp-user.delete",
                                                                  type = ExchangeTypes.TOPIC),
                                             key = "pm"))
    public void delete(@Payload SimpleServiceMessage message, @Headers Map<String, String> headers) {
        String provider = headers.get("provider");
        logger.debug("Received delete message from " + provider + ": " + message.toString());

        try {
            State state = businessFlowDirector.processMessage(message);

            if (state.equals(State.PROCESSED)) {
                //Save history
                Map<String, String> params = new HashMap<>();
                params.put(HISTORY_MESSAGE_KEY, "Заявка на удаление FTP-пользователя выполнена успешно (имя: " + message.getParam("name") + ")");
                params.put(OPERATOR_KEY, "service");

                publisher.publishEvent(new AccountHistoryEvent(message.getAccountId(), params));
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Got Exception in ru.majordomo.hms.personmgr.controller.amqp.FtpUserAmqpController.delete " + e.getMessage());
        }
    }
}
