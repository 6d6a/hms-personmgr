package ru.majordomo.hms.personmgr.controller.amqp;

import org.springframework.amqp.core.Message;
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
import ru.majordomo.hms.personmgr.manager.CartManager;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessAction;
import ru.majordomo.hms.personmgr.service.AccountStatHelper;

import static ru.majordomo.hms.personmgr.common.AccountStatType.VIRTUAL_HOSTING_AUTO_RENEW_DOMAIN;
import static ru.majordomo.hms.personmgr.common.Constants.AUTO_RENEW_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.Exchanges.DOMAIN_CREATE;
import static ru.majordomo.hms.personmgr.common.Constants.Exchanges.DOMAIN_DELETE;
import static ru.majordomo.hms.personmgr.common.Constants.Exchanges.DOMAIN_UPDATE;
import static ru.majordomo.hms.personmgr.common.Constants.HISTORY_MESSAGE_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.OPERATOR_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.RESOURCE_ID_KEY;

@Service
public class DomainAmqpController extends CommonAmqpController {
    private final CartManager cartManager;
    private final AccountStatHelper accountStatHelper;

    @Autowired
    public DomainAmqpController(
            CartManager cartManager,
            AccountStatHelper accountStatHelper
    ) {
        this.cartManager = cartManager;
        this.accountStatHelper = accountStatHelper;
        resourceName = "домен";
    }

    @RabbitListener(queues = "${hms.instance.name}" + "." + "${spring.application.name}" + "." + DOMAIN_CREATE)
    public void create(Message amqpMessage, @Payload SimpleServiceMessage message, @Headers Map<String, String> headers) {
        String provider = headers.get("provider");
        logger.debug("Received from " + provider + ": " + message.toString());

        try {
            State state = businessFlowDirector.processMessage(message);
            ProcessingBusinessAction businessAction = processingBusinessActionRepository.findOne(message.getActionIdentity());

            if (businessAction != null) {
                String domainName = (String) businessAction.getParam("name");

                if (state == State.PROCESSED) {
                    PersonalAccount account = accountManager.findOne(businessAction.getPersonalAccountId());

                    if (businessAction.getBusinessActionType().equals(BusinessActionType.DOMAIN_CREATE_RC)) {
                        if (account.isAccountNew()) {
                            accountManager.setAccountNew(account.getId(), false);
                        }
                        if ((Boolean) businessAction.getParam("register")) {
                            HashMap<String, String> data = new HashMap<>();
                            data.put("personId", (String) businessAction.getParam("personId"));
                            data.put("domainName", domainName);
                            accountStatHelper.add(account.getId(), AccountStatType.VIRTUAL_HOSTING_REGISTER_DOMAIN, data);
                        }
                    }

                    cartManager.deleteCartItemByName(businessAction.getPersonalAccountId(), domainName);

                    //Save history
                    Map<String, String> params = new HashMap<>();
                    params.put(HISTORY_MESSAGE_KEY, "Заявка на создание домена выполнена успешно (имя: " + domainName + ")");
                    params.put(OPERATOR_KEY, "service");

                    publisher.publishEvent(new AccountHistoryEvent(businessAction.getPersonalAccountId(), params));
                } else {
                    cartManager.setProcessingByName(businessAction.getPersonalAccountId(), domainName, false);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Got Exception in DomainAmqpController.create " + e.getMessage());
        }
    }

    @RabbitListener(queues = "${hms.instance.name}" + "." + "${spring.application.name}" + "." + DOMAIN_UPDATE)
    public void update(Message amqpMessage, @Payload SimpleServiceMessage message, @Headers Map<String, String> headers) {
        String provider = headers.get("provider");
        logger.debug("Received update message from " + provider + ": " + message.toString());

        try {
            State state = businessFlowDirector.processMessage(message);

            if (state == State.PROCESSED) {
                ProcessingBusinessAction businessAction = processingBusinessActionRepository.findOne(message.getActionIdentity());

                if (businessAction != null) {
                    String domainName = (String) businessAction.getParam("name");

                    Map<String, String> paramsHistory;
                    if (businessAction.getBusinessActionType().equals(BusinessActionType.DOMAIN_UPDATE_RC)
                            && businessAction.getParam("renew") != null
                            && (Boolean) businessAction.getParam("renew")
                            ) {
                        String renewAction = "продление";
                        boolean statDataAutoRenew = false;

                        if (businessAction.getParam(AUTO_RENEW_KEY) != null &&
                                (Boolean) businessAction.getParam(AUTO_RENEW_KEY)
                                ) {
                            Map<String, String> params = new HashMap<>();
                            params.put(RESOURCE_ID_KEY, (String) businessAction.getParam(RESOURCE_ID_KEY));

                            publisher.publishEvent(new AccountDomainAutoRenewCompletedEvent(businessAction.getPersonalAccountId(), params));
                            renewAction = "автопродление";
                            statDataAutoRenew = true;
                        }

                        HashMap<String, String> statData = new HashMap<>();
                        statData.put("personId", (String) businessAction.getParam("personId"));
                        statData.put("domainName", domainName);
                        accountStatHelper.add(
                                message.getAccountId(),
                                statDataAutoRenew ? VIRTUAL_HOSTING_AUTO_RENEW_DOMAIN : AccountStatType.VIRTUAL_HOSTING_MANUAL_RENEW_DOMAIN,
                                statData);

                        //Save history
                        paramsHistory = new HashMap<>();
                        paramsHistory.put(HISTORY_MESSAGE_KEY, "Заявка на " + renewAction + " домена выполнена успешно (имя: " + domainName + ")");
                        paramsHistory.put(OPERATOR_KEY, "service");

                        publisher.publishEvent(new AccountHistoryEvent(businessAction.getPersonalAccountId(), paramsHistory));
                    } else {
                        //Save history
                        paramsHistory = new HashMap<>();
                        paramsHistory.put(HISTORY_MESSAGE_KEY, "Заявка на обновление домена выполнена успешно (имя: " + domainName + ")");
                        paramsHistory.put(OPERATOR_KEY, "service");

                        publisher.publishEvent(new AccountHistoryEvent(businessAction.getPersonalAccountId(), paramsHistory));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Got Exception in DomainAmqpController.update " + e.getMessage());
        }
    }

    @RabbitListener(queues = "${hms.instance.name}" + "." + "${spring.application.name}" + "." + DOMAIN_DELETE)
    public void delete(Message amqpMessage, @Payload SimpleServiceMessage message, @Headers Map<String, String> headers) {
        handleDeleteEventFromRc(message, headers);
    }
}
