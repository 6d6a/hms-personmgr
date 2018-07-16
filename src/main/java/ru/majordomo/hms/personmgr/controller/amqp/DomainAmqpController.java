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
import ru.majordomo.hms.personmgr.event.accountStat.AccountStatDomainUpdateEvent;
import ru.majordomo.hms.personmgr.manager.CartManager;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessAction;
import ru.majordomo.hms.personmgr.service.AccountStatHelper;

import static ru.majordomo.hms.personmgr.common.Constants.Exchanges.DOMAIN_CREATE;
import static ru.majordomo.hms.personmgr.common.Constants.Exchanges.DOMAIN_DELETE;
import static ru.majordomo.hms.personmgr.common.Constants.Exchanges.DOMAIN_UPDATE;
import static ru.majordomo.hms.personmgr.common.Constants.*;

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

            if (businessAction == null) {
                return;
            }
            String domainName = (String) businessAction.getParam("name");

            if (state == State.PROCESSED) {
                PersonalAccount account = accountManager.findOne(businessAction.getPersonalAccountId());

                if (businessAction.getBusinessActionType().equals(BusinessActionType.DOMAIN_CREATE_RC)) {
                    if (account.isAccountNew()) {
                        accountManager.setAccountNew(account.getId(), false);
                    }
                    if (businessAction.getParam("register") != null && (boolean) businessAction.getParam("register")) {
                        HashMap<String, String> data = new HashMap<>();
                        data.put("personId", (String) businessAction.getParam("personId"));
                        data.put(DOMAIN_NAME_KEY, domainName);
                        data.put(ACCOUNT_ID_KEY, message.getAccountId());
                        accountStatHelper.add(account.getId(), AccountStatType.VIRTUAL_HOSTING_REGISTER_DOMAIN, data);
                    }
                }

                cartManager.deleteCartItemByName(businessAction.getPersonalAccountId(), domainName);

                history.save(businessAction.getPersonalAccountId(),
                        "Заявка на создание домена выполнена успешно (имя: " + domainName + ")",
                        "service");
            } else {
                cartManager.setProcessingByName(businessAction.getPersonalAccountId(), domainName, false);
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

            if (state != State.PROCESSED) {
                logger.error("State is not PROCESSED" );
                return;
            }

            ProcessingBusinessAction businessAction = processingBusinessActionRepository.findOne(message.getActionIdentity());

            if (businessAction == null) {
                logger.error("Не найден ProcessingBusinessAction с actionIdentity " + message.getActionIdentity());
                return;
            }

            String renewAction = "обновление";
            String domainName = (String) message.getParam(NAME_KEY);
            if (businessAction.getBusinessActionType().equals(BusinessActionType.DOMAIN_UPDATE_RC)
                    && businessAction.getParam("renew") != null
                    && (Boolean) businessAction.getParam("renew")
            ) {
                renewAction = "продление";
                boolean statDataAutoRenew = false;

                if (businessAction.getParam(AUTO_RENEW_KEY) != null &&
                        (Boolean) businessAction.getParam(AUTO_RENEW_KEY)
                ) {
                    renewAction = "автопродление";
                    statDataAutoRenew = true;
                }
                message.addParam(AUTO_RENEW_KEY, statDataAutoRenew);
                publisher.publishEvent(new AccountStatDomainUpdateEvent(message));
            }

            history.save(businessAction.getPersonalAccountId(),
                    "Заявка на " + renewAction + " домена выполнена успешно (имя: " + domainName + ")",
                    "service");

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
