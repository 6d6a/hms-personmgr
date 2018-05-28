package ru.majordomo.hms.personmgr.controller.amqp;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;

import java.util.Map;

import static ru.majordomo.hms.personmgr.common.Constants.Exchanges.REDIRECT_CREATE;
import static ru.majordomo.hms.personmgr.common.Constants.Exchanges.REDIRECT_DELETE;
import static ru.majordomo.hms.personmgr.common.Constants.Exchanges.REDIRECT_UPDATE;

@Service
public class RedirectAmqpController extends CommonAmqpController  {

    public RedirectAmqpController() {
        resourceName = "перенаправление";
    }

    @RabbitListener(queues = "${hms.instance.name}" + "." + "${spring.application.name}" + "." + REDIRECT_CREATE)
    public void create(Message amqpMessage, @Payload SimpleServiceMessage message, @Headers Map<String, String> headers) {
        handleCreateEventFromRc(message, headers);
    }

    @RabbitListener(queues = "${hms.instance.name}" + "." + "${spring.application.name}" + "." + REDIRECT_UPDATE)
    public void update(Message amqpMessage, @Payload SimpleServiceMessage message, @Headers Map<String, String> headers) {
        handleUpdateEventFromRc(message, headers);
    }

    @RabbitListener(queues = "${hms.instance.name}" + "." + "${spring.application.name}" + "." + REDIRECT_DELETE)
    public void delete(Message amqpMessage, @Payload SimpleServiceMessage message, @Headers Map<String, String> headers) {
        handleDeleteEventFromRc(message, headers);
    }
}
