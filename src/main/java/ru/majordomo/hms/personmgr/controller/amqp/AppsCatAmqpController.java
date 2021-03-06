package ru.majordomo.hms.personmgr.controller.amqp;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

import ru.majordomo.hms.personmgr.common.BusinessActionType;
import ru.majordomo.hms.personmgr.common.State;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessAction;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessOperation;

import static ru.majordomo.hms.personmgr.common.Constants.APPSCAT_ADMIN_PASSWORD_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.APPSCAT_ADMIN_USERNAME_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.APPSCAT_DOMAIN_NAME_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.Exchanges.APPS_CAT_INSTALL;

@Service
public class AppsCatAmqpController extends CommonAmqpController {
    @RabbitListener(queues = "${hms.instance.name}" + "." + "${spring.application.name}" + "." + APPS_CAT_INSTALL)
    public void install(Message amqpMessage, @Payload SimpleServiceMessage message, @Headers Map<String, String> headers) {
        String provider = headers.get("provider");
        logger.debug("Received from " + provider + ": " + message.toString());

        try {
            processingBusinessOperationRepository.findById(message.getOperationIdentity()).ifPresent(businessOperation -> {
                List<ProcessingBusinessAction> businessActions = processingBusinessActionRepository.findAllByOperationId(businessOperation.getId());

                businessActions
                        .stream()
                        .filter(processingBusinessAction -> processingBusinessAction.getBusinessActionType() == BusinessActionType.APP_INSTALL_APPSCAT)
                        .findFirst()
                        .ifPresent(processingBusinessAction -> message.setActionIdentity(processingBusinessAction.getId()));

                State state = businessFlowDirector.processMessage(message, resourceName);

                businessOperation.setState(state);

                //?????????????? ?????? ?????????? ?????????? ?????????????????????? ?? ???????????? ???????????? ???? ?????????????????? (???? ?????????? ?????????????? ?????? ?????? DB, ???????? ?????? DB-??????????)
                businessOperation.addPublicParam("name", message.getParam(APPSCAT_DOMAIN_NAME_KEY));
                businessOperation.addPublicParam(APPSCAT_ADMIN_USERNAME_KEY, message.getParam(APPSCAT_ADMIN_USERNAME_KEY));
                businessOperation.addPublicParam(APPSCAT_ADMIN_PASSWORD_KEY, message.getParam(APPSCAT_ADMIN_PASSWORD_KEY));
                businessOperation.addPublicParam(APPSCAT_DOMAIN_NAME_KEY, message.getParam(APPSCAT_DOMAIN_NAME_KEY));

                processingBusinessOperationRepository.save(businessOperation);

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
                        history.save(
                                businessOperation.getPersonalAccountId(),
                                "???????????? ???? ?????????????????? ???????????????????? ?????????????????? ?????????????? (??????: " + businessOperation.getPublicParam("name") + ")",
                                "service"
                        );
                        break;
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Got Exception in AppsCatAmqpController.install " + e.getMessage());
        }
    }
}
