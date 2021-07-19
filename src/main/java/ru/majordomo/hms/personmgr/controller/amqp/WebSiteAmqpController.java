package ru.majordomo.hms.personmgr.controller.amqp;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.Map;

import ru.majordomo.hms.personmgr.common.BusinessActionType;
import ru.majordomo.hms.personmgr.common.ResourceType;
import ru.majordomo.hms.personmgr.common.UserConstants;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;

import static ru.majordomo.hms.personmgr.common.Constants.APPSCAT_ROUTING_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.ERROR_MESSAGE_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.Exchanges.WEBSITE_CREATE;
import static ru.majordomo.hms.personmgr.common.Constants.Exchanges.WEBSITE_DELETE;
import static ru.majordomo.hms.personmgr.common.Constants.Exchanges.WEBSITE_UPDATE;
import static ru.majordomo.hms.personmgr.common.Constants.SUCCESS_KEY;

@Service
public class WebSiteAmqpController extends CommonAmqpController  {

    public WebSiteAmqpController() {
        resourceName = UserConstants.WEB_SITE;
    }

    @RabbitListener(queues = "${hms.instance.name}" + "." + "${spring.application.name}" + "." + WEBSITE_CREATE)
    public void create(Message amqpMessage, @Payload SimpleServiceMessage message, @Headers Map<String, String> headers) {
        handleCreateEventFromRc(message, headers);
    }

    @RabbitListener(queues = "${hms.instance.name}" + "." + "${spring.application.name}" + "." + WEBSITE_UPDATE)
    public void update(Message amqpMessage, @Payload SimpleServiceMessage message, @Headers Map<String, String> headers) {
        String provider = headers.get("provider");
        String realProviderName = provider.replaceAll("^" + instanceName + "\\.", "");
        if (realProviderName.equals("rc-user")) {
            handleUpdateEventFromRc(message, headers);
        } else  if (realProviderName.startsWith("te.")) {
            handleUpdateEventFromTE(message, headers);
        } else if (realProviderName.equals(APPSCAT_ROUTING_KEY)) {
            try {
                PersonalAccount account = accountManager.findOne(message.getAccountId());

                resourceChecker.checkResource(account, ResourceType.WEB_SITE, message.getParams());
            } catch (Exception e) {
                e.printStackTrace();
                message.addParam(SUCCESS_KEY, false);
                message.addParam(ERROR_MESSAGE_KEY, e.getMessage());

                try {
                    amqpSender.send(WEBSITE_UPDATE, APPSCAT_ROUTING_KEY, message);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                
                return;
            }

            businessHelper.buildActionByOperationId(BusinessActionType.WEB_SITE_UPDATE_RC, message, message.getOperationIdentity());
        }
    }

    @RabbitListener(queues = "${hms.instance.name}" + "." + "${spring.application.name}" + "." + WEBSITE_DELETE)
    public void delete(Message amqpMessage, @Payload SimpleServiceMessage message, @Headers Map<String, String> headers) {
        handleDeleteEventFromRc(message, headers);
    }
}
