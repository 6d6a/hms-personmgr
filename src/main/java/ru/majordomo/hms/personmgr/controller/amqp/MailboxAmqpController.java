package ru.majordomo.hms.personmgr.controller.amqp;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.Map;

import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;

import static ru.majordomo.hms.personmgr.common.Constants.Exchanges.MAILBOX_CREATE;
import static ru.majordomo.hms.personmgr.common.Constants.Exchanges.MAILBOX_DELETE;
import static ru.majordomo.hms.personmgr.common.Constants.Exchanges.MAILBOX_UPDATE;

@Service
public class MailboxAmqpController extends CommonAmqpController {
    public MailboxAmqpController() {
        resourceName = "почтовый ящик";
    }

    @RabbitListener(queues = "${hms.instance.name}" + "." + "${spring.application.name}" + "." + MAILBOX_CREATE)
    public void create(@Payload SimpleServiceMessage message, @Headers Map<String, String> headers) {
        handleCreateEventFromRc(message, headers);
    }

    @RabbitListener(queues = "${hms.instance.name}" + "." + "${spring.application.name}" + "." + MAILBOX_UPDATE)
    public void update(@Payload SimpleServiceMessage message, @Headers Map<String, String> headers) {
        handleUpdateEventFromRc(message, headers);
    }

    @RabbitListener(queues = "${hms.instance.name}" + "." + "${spring.application.name}" + "." + MAILBOX_DELETE)
    public void delete(@Payload SimpleServiceMessage message, @Headers Map<String, String> headers) {
        handleDeleteEventFromRc(message, headers);
    }
}
