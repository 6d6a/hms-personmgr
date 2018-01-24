package ru.majordomo.hms.personmgr.controller.amqp;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.Map;

import ru.majordomo.hms.personmgr.common.BusinessActionType;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.service.BusinessHelper;

import static ru.majordomo.hms.personmgr.common.Constants.Exchanges.WEBSITE_CREATE;
import static ru.majordomo.hms.personmgr.common.Constants.Exchanges.WEBSITE_DELETE;
import static ru.majordomo.hms.personmgr.common.Constants.Exchanges.WEBSITE_UPDATE;

@Service
public class WebSiteAmqpController extends CommonAmqpController  {
    private final BusinessHelper businessHelper;

    public WebSiteAmqpController(BusinessHelper businessHelper) {
        this.businessHelper = businessHelper;
        resourceName = "сайт";
    }

    @RabbitListener(queues = "${hms.instance.name}" + "." + "${spring.application.name}" + "." + WEBSITE_CREATE)
    public void create(Message amqpMessage, @Payload SimpleServiceMessage message, @Headers Map<String, String> headers) {
        handleCreateEventFromRc(message, headers);
    }

    @RabbitListener(queues = "${hms.instance.name}" + "." + "${spring.application.name}" + "." + WEBSITE_UPDATE)
    public void update(Message amqpMessage, @Payload SimpleServiceMessage message, @Headers Map<String, String> headers) {
        String provider = headers.get("provider");
        String realProviderName = provider.replaceAll("^" + instanceName + "\\.", "");
        switch (realProviderName) {
            case "rc-user":
                handleUpdateEventFromRc(message, headers);

                break;
            case "appscat":
                businessHelper.buildActionByOperationId(BusinessActionType.WEB_SITE_UPDATE_RC, message, message.getOperationIdentity());

                break;
        }
    }

    @RabbitListener(queues = "${hms.instance.name}" + "." + "${spring.application.name}" + "." + WEBSITE_DELETE)
    public void delete(Message amqpMessage, @Payload SimpleServiceMessage message, @Headers Map<String, String> headers) {
        handleDeleteEventFromRc(message, headers);
    }
}
