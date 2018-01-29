package ru.majordomo.hms.personmgr.controller.amqp;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.majordomo.hms.personmgr.common.BusinessActionType;
import ru.majordomo.hms.personmgr.common.State;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.event.accountHistory.AccountHistoryEvent;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessAction;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessOperation;

import static ru.majordomo.hms.personmgr.common.Constants.APPSCAT_ADMIN_PASSWORD_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.APPSCAT_ADMIN_USERNAME_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.APPSCAT_DOMAIN_NAME_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.Exchanges.APPS_CAT_INSTALL;
import static ru.majordomo.hms.personmgr.common.Constants.HISTORY_MESSAGE_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.OPERATOR_KEY;

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
                businessOperation.addPublicParam(APPSCAT_ADMIN_USERNAME_KEY, businessOperation.getParam(APPSCAT_ADMIN_USERNAME_KEY));
                businessOperation.addPublicParam(APPSCAT_ADMIN_PASSWORD_KEY, businessOperation.getParam(APPSCAT_ADMIN_PASSWORD_KEY));
                businessOperation.addPublicParam(APPSCAT_DOMAIN_NAME_KEY, businessOperation.getParam(APPSCAT_DOMAIN_NAME_KEY));

                processingBusinessOperationRepository.save(businessOperation);

                List<ProcessingBusinessAction> businessActions = processingBusinessActionRepository.findAllByOperationId(businessOperation.getId());
                businessActions
                        .stream()
                        .filter(processingBusinessAction -> processingBusinessAction.getBusinessActionType() == BusinessActionType.APP_INSTALL_APPSCAT)
                        .findFirst()
                        .ifPresent(processingBusinessAction -> {
                            processingBusinessAction.setState(state);
                            processingBusinessActionRepository.save(processingBusinessAction);
                        });

                switch (state) {
                    case PROCESSED:
                        try {
                            //Save history
                            Map<String, String> params = new HashMap<>();
                            params.put(HISTORY_MESSAGE_KEY, "Заявка на установку приложения выполнена успешно (имя: " +
                                    businessOperation.getPublicParam("name") + ")");
                            params.put(OPERATOR_KEY, "service");

                            publisher.publishEvent(new AccountHistoryEvent(businessOperation.getPersonalAccountId(), params));
                        } catch (Exception e) {
                            e.printStackTrace();
                            logger.error("Got Exception in AppsCatService.finishInstall " + e.getMessage());
                        }

                        break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Got Exception in AppsCatAmqpController.install " + e.getMessage());
        }
    }
}
