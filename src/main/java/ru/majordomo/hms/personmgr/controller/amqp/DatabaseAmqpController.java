package ru.majordomo.hms.personmgr.controller.amqp;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.Map;

import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;

import static ru.majordomo.hms.personmgr.common.Constants.Exchanges.DATABASE_CREATE;
import static ru.majordomo.hms.personmgr.common.Constants.Exchanges.DATABASE_DELETE;
import static ru.majordomo.hms.personmgr.common.Constants.Exchanges.DATABASE_UPDATE;

@Service
public class DatabaseAmqpController extends CommonAmqpController {

    public DatabaseAmqpController() {
        resourceName = "база данных";
    }

    @RabbitListener(queues = "${hms.instance.name}" + "." + "${spring.application.name}" + "." + DATABASE_CREATE)
    public void create(@Payload SimpleServiceMessage message, @Headers Map<String, String> headers) {
        handleCreateEventFromRc(message, headers);
    }

    @RabbitListener(queues = "${hms.instance.name}" + "." + "${spring.application.name}" + "." + DATABASE_UPDATE)
    public void update(@Payload SimpleServiceMessage message, @Headers Map<String, String> headers) {
        handleUpdateEventFromRc(message, headers);
    }

    @RabbitListener(queues = "${hms.instance.name}" + "." + "${spring.application.name}" + "." + DATABASE_DELETE)
    public void delete(@Payload SimpleServiceMessage message, @Headers Map<String, String> headers) {
        handleDeleteEventFromRc(message, headers);
    }
}
