package ru.majordomo.hms.personmgr.controller.amqp;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

import ru.majordomo.hms.personmgr.common.State;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.dto.AppscatApp;
import ru.majordomo.hms.personmgr.event.account.AccountAppInstalledEvent;
import ru.majordomo.hms.personmgr.event.accountHistory.AccountHistoryEvent;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessOperation;
import ru.majordomo.hms.personmgr.service.AppscatFeignClient;

import static ru.majordomo.hms.personmgr.common.Constants.APPSCAT_ADMIN_PASSWORD_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.APPSCAT_DOMAIN_NAME_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.APP_ID_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.Exchanges.APPS_CAT_INSTALL;
import static ru.majordomo.hms.personmgr.common.Constants.HISTORY_MESSAGE_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.OPERATOR_KEY;

@Service
public class AppsCatAmqpController extends CommonAmqpController {
    private final AppscatFeignClient appscatFeignClient;

    public AppsCatAmqpController(AppscatFeignClient appscatFeignClient) {
        this.appscatFeignClient = appscatFeignClient;
    }

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

                String logMessage = "ACTION_IDENTITY: " + message.getActionIdentity() +
                        " OPERATION_IDENTITY: " + message.getOperationIdentity() +
                        " установка приложения на аккаунт " + message.getAccountId();

                switch (state) {
                    case PROCESSED:
                        logger.info(logMessage + " завершена успешно");

                        try {
                            //Save history
                            Map<String, String> params = new HashMap<>();
                            params.put(HISTORY_MESSAGE_KEY, "Заявка на установку приложения выполнена успешно (имя: " + message.getParam("name") + ")");
                            params.put(OPERATOR_KEY, "service");

                            publisher.publishEvent(new AccountHistoryEvent(message.getAccountId(), params));

                            PersonalAccount account = accountManager.findOne(message.getAccountId());

                            AppscatApp app = appscatFeignClient.getApp((String) businessOperation.getParam(APP_ID_KEY));

                            if (app != null) {
                                String appUri = (String) businessOperation.getParam(APPSCAT_DOMAIN_NAME_KEY);
                                Map<String, String> paramsEmail = new HashMap<>();
                                paramsEmail.put("app_name", app.getName() + " " + app.getVersion());
                                paramsEmail.put("site_name", appUri);
                                paramsEmail.put("app_admin_uri", appUri + app.getAdminUri());
                                paramsEmail.put("app_admin_password", (String) businessOperation.getParam(APPSCAT_ADMIN_PASSWORD_KEY));

                                publisher.publishEvent(new AccountAppInstalledEvent(account, paramsEmail));
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            logger.error("Got Exception in AppsCatAmqpController.install " + e.getMessage());
                        }

                        break;
                    case ERROR:
                        logger.error(logMessage + " не удалась");
                        break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Got Exception in AppsCatAmqpController.install " + e.getMessage());
        }
    }
}
