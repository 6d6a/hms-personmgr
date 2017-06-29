package ru.majordomo.hms.personmgr.controller.amqp;

import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

import ru.majordomo.hms.personmgr.common.AccountStatType;
import ru.majordomo.hms.personmgr.common.BusinessActionType;
import ru.majordomo.hms.personmgr.common.State;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.event.account.AccountDomainAutoRenewCompletedEvent;
import ru.majordomo.hms.personmgr.event.accountHistory.AccountHistoryEvent;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessAction;
import ru.majordomo.hms.personmgr.repository.ProcessingBusinessActionRepository;
import ru.majordomo.hms.personmgr.service.AccountStatHelper;

import static ru.majordomo.hms.personmgr.common.Constants.AUTO_RENEW_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.HISTORY_MESSAGE_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.OPERATOR_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.RESOURCE_ID_KEY;

@EnableRabbit
@Service
public class DomainAmqpController extends CommonAmqpController {
    private final ProcessingBusinessActionRepository processingBusinessActionRepository;
    private final AccountStatHelper accountStatHelper;

    @Autowired
    public DomainAmqpController(
            ProcessingBusinessActionRepository processingBusinessActionRepository,
            AccountStatHelper accountStatHelper
    ) {
        this.processingBusinessActionRepository = processingBusinessActionRepository;
        this.accountStatHelper = accountStatHelper;
    }

    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(
                            value = "pm.domain.create",
                            durable = "true",
                            autoDelete = "false"
                    ),
                    exchange = @Exchange(
                            value = "domain.create",
                            type = ExchangeTypes.TOPIC
                    ),
                    key = "pm"
            )
    )
    public void create(@Payload SimpleServiceMessage message, @Headers Map<String, String> headers) {
        String provider = headers.get("provider");
        logger.debug("Received from " + provider + ": " + message.toString());

        try {
            State state = businessFlowDirector.processMessage(message);

            if (state == State.PROCESSED) {
                ProcessingBusinessAction businessAction = processingBusinessActionRepository.findOne(message.getActionIdentity());

                if (businessAction != null && businessAction.getBusinessActionType().equals(BusinessActionType.DOMAIN_CREATE_RC)) {
                    PersonalAccount account = accountManager.findOne(businessAction.getPersonalAccountId());
                    if (account.isAccountNew()) {
                        accountManager.setAccountNew(account.getAccountId(), false);
                    }
                }

                PersonalAccount account = accountManager.findOne(businessAction.getPersonalAccountId());

                if (businessAction.getParam("register") == "true") {
                    accountStatHelper.add(account, AccountStatType.VIRTUAL_HOSTING_REGISTER_DOMAIN);
                }
                //Save history
                Map<String, String> params = new HashMap<>();
                params.put(HISTORY_MESSAGE_KEY, "Заявка на создание домена выполнена успешно (имя: " + message.getParam("name") + ")");
                params.put(OPERATOR_KEY, "service");

                publisher.publishEvent(new AccountHistoryEvent(message.getAccountId(), params));
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Got Exception in ru.majordomo.hms.personmgr.controller.amqp.DomainAmqpController.create " + e.getMessage());
        }
    }

    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(
                            value = "pm.domain.update",
                            durable = "true",
                            autoDelete = "false"
                    ),
                    exchange = @Exchange(
                            value = "domain.update",
                            type = ExchangeTypes.TOPIC
                    ),
                    key = "pm"
            )
    )
    public void update(@Payload SimpleServiceMessage message, @Headers Map<String, String> headers) {
        String provider = headers.get("provider");
        logger.debug("Received update message from " + provider + ": " + message.toString());

        try {
            State state = businessFlowDirector.processMessage(message);

            if (state == State.PROCESSED) {
                ProcessingBusinessAction businessAction = processingBusinessActionRepository.findOne(message.getActionIdentity());

                if (businessAction != null) {
                    PersonalAccount account = accountManager.findOne(businessAction.getPersonalAccountId());

                    Map<String, String> paramsHistory;
                    if (businessAction.getBusinessActionType().equals(BusinessActionType.DOMAIN_UPDATE_RC)
                            && businessAction.getParam("renew") != null
                            && (Boolean) businessAction.getParam("renew")
                            ) {
                        String renewAction = "продление";

                        if (businessAction.getParam(AUTO_RENEW_KEY) != null &&
                                (Boolean) businessAction.getParam(AUTO_RENEW_KEY)
                                ) {
                            Map<String, String> params = new HashMap<>();
                            params.put(RESOURCE_ID_KEY, (String) businessAction.getParam(RESOURCE_ID_KEY));

                            publisher.publishEvent(new AccountDomainAutoRenewCompletedEvent(account, params));
                            renewAction = "автопродление";
                        }

                        //Save history
                        paramsHistory = new HashMap<>();
                        paramsHistory.put(HISTORY_MESSAGE_KEY, "Заявка на " + renewAction + " домена выполнена успешно (имя: " + message.getParam("name") + ")");
                        paramsHistory.put(OPERATOR_KEY, "service");

                        publisher.publishEvent(new AccountHistoryEvent(account.getId(), paramsHistory));
                    } else {
                        //Save history
                        paramsHistory = new HashMap<>();
                        paramsHistory.put(HISTORY_MESSAGE_KEY, "Заявка на обновление домена выполнена успешно (имя: " + message.getParam("name") + ")");
                        paramsHistory.put(OPERATOR_KEY, "service");

                        publisher.publishEvent(new AccountHistoryEvent(account.getId(), paramsHistory));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Got Exception in ru.majordomo.hms.personmgr.controller.amqp.DomainAmqpController.update " + e.getMessage());
        }
    }

    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(
                            value = "pm.domain.delete",
                            durable = "true",
                            autoDelete = "false"
                    ),
                    exchange = @Exchange(
                            value = "domain.delete",
                            type = ExchangeTypes.TOPIC
                    ),
                    key = "pm"
            )
    )
    public void delete(@Payload SimpleServiceMessage message, @Headers Map<String, String> headers) {
        String provider = headers.get("provider");
        logger.debug("Received delete message from " + provider + ": " + message.toString());

        try {
            State state = businessFlowDirector.processMessage(message);

            if (state.equals(State.PROCESSED)) {
                //Save history
                Map<String, String> params = new HashMap<>();
                params.put(HISTORY_MESSAGE_KEY, "Заявка на удаление домена выполнена успешно (имя: " + message.getParam("name") + ")");
                params.put(OPERATOR_KEY, "service");

                publisher.publishEvent(new AccountHistoryEvent(message.getAccountId(), params));
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Got Exception in ru.majordomo.hms.personmgr.controller.amqp.DomainAmqpController.delete " + e.getMessage());
        }
    }
}
