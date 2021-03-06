package ru.majordomo.hms.personmgr.controller.amqp;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import ru.majordomo.hms.personmgr.common.UserConstants;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import java.util.Map;

import static ru.majordomo.hms.personmgr.common.Constants.Exchanges.RESOURCE_ARCHIVE_CREATE;
import static ru.majordomo.hms.personmgr.common.Constants.Exchanges.RESOURCE_ARCHIVE_DELETE;
import static ru.majordomo.hms.personmgr.common.Constants.Exchanges.RESOURCE_ARCHIVE_UPDATE;

@Service
public class ResourceArchiveAmqpController extends CommonAmqpController {
    public ResourceArchiveAmqpController() {
        resourceName = UserConstants.RESOURCE_ARCHIVE;
    }

    @RabbitListener(queues = "${hms.instance.name}" + "." + "${spring.application.name}" + "." + RESOURCE_ARCHIVE_CREATE)
    public void create(Message amqpMessage, @Payload SimpleServiceMessage message, @Headers Map<String, String> headers) {
        handleCreateEventFromRc(message, headers);
    }

    @RabbitListener(queues = "${hms.instance.name}" + "." + "${spring.application.name}" + "." + RESOURCE_ARCHIVE_UPDATE)
    public void update(Message amqpMessage, @Payload SimpleServiceMessage message, @Headers Map<String, String> headers) {
        handleUpdateEventFromRc(message, headers);
    }

    @RabbitListener(queues = "${hms.instance.name}" + "." + "${spring.application.name}" + "." + RESOURCE_ARCHIVE_DELETE)
    public void delete(Message amqpMessage, @Payload SimpleServiceMessage message, @Headers Map<String, String> headers) {
        handleDeleteEventFromRc(message, headers);
    }
}
