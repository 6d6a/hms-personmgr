package ru.majordomo.hms.personmgr.controller.amqp;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.Map;

import ru.majordomo.hms.personmgr.common.State;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessOperation;

import static ru.majordomo.hms.personmgr.common.Constants.APPSCAT_DOMAIN_NAME_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.Exchanges.APPS_CAT_INSTALL;

@Service
public class AppsCatAmqpController extends CommonAmqpController {
    @RabbitListener(queues = "${hms.instance.name}" + "." + "${spring.application.name}" + "." + APPS_CAT_INSTALL)
    public void install(Message amqpMessage, @Payload SimpleServiceMessage message, @Headers Map<String, String> headers) {
        String provider = headers.get("provider");
        logger.debug("Received from " + provider + ": " + message.toString());

        try {
            State state = businessFlowDirector.processMessage(message);

            ProcessingBusinessOperation businessOperation = processingBusinessOperationRepository.findOne(message.getOperationIdentity());
            if (businessOperation != null) {
                businessOperation.setState(state);

                //Запишем урл сайта чтобы отображался в случае ошибки во фронтэнде (до этого момента там имя DB, либо имя DB-юзера)
                businessOperation.addPublicParam("name", businessOperation.getParam(APPSCAT_DOMAIN_NAME_KEY));

                processingBusinessOperationRepository.save(businessOperation);
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Got Exception in AppsCatAmqpController.install " + e.getMessage());
        }
    }
}
