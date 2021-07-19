package ru.majordomo.hms.personmgr.controller.amqp;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.Map;

import ru.majordomo.hms.personmgr.common.BusinessActionType;
import ru.majordomo.hms.personmgr.common.UserConstants;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;

import static ru.majordomo.hms.personmgr.common.Constants.Exchanges.DATABASE_USER_CREATE;
import static ru.majordomo.hms.personmgr.common.Constants.Exchanges.DATABASE_USER_DELETE;
import static ru.majordomo.hms.personmgr.common.Constants.Exchanges.DATABASE_USER_UPDATE;

@Service
public class DatabaseUserAmqpController extends CommonAmqpController {
    public DatabaseUserAmqpController() {
        resourceName = UserConstants.DATABASE_USER;
    }

    @RabbitListener(queues = "${hms.instance.name}" + "." + "${spring.application.name}" + "." + DATABASE_USER_CREATE)
    public void create(Message amqpMessage, @Payload SimpleServiceMessage message, @Headers Map<String, String> headers) {
        String provider = headers.get("provider");
        String realProviderName = provider.replaceAll("^" + instanceName + "\\.", "");
        switch (realProviderName) {
            case "rc-user":
                handleCreateEventFromRc(message, headers);

                break;
            case "appscat":
                businessHelper.buildActionByOperationId(BusinessActionType.DATABASE_USER_CREATE_RC, message, message.getOperationIdentity());

                break;
        }
    }

    @RabbitListener(queues = "${hms.instance.name}" + "." + "${spring.application.name}" + "." + DATABASE_USER_UPDATE)
    public void update(Message amqpMessage, @Payload SimpleServiceMessage message, @Headers Map<String, String> headers) {
        handleUpdateEventFromRc(message, headers);
    }

    @RabbitListener(queues = "${hms.instance.name}" + "." + "${spring.application.name}" + "." + DATABASE_USER_DELETE)
    public void delete(Message amqpMessage, @Payload SimpleServiceMessage message, @Headers Map<String, String> headers) {
        handleDeleteEventFromRc(message, headers);
    }
}
