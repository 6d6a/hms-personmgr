package ru.majordomo.hms.personmgr.controller.amqp;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

import ru.majordomo.hms.personmgr.common.*;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.dto.domainTransfer.DomainTransferSynchronization;
import ru.majordomo.hms.personmgr.event.accountStat.AccountStatDomainUpdateEvent;
import ru.majordomo.hms.personmgr.manager.CartManager;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessAction;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessOperation;
import ru.majordomo.hms.personmgr.service.AccountStatHelper;
import ru.majordomo.hms.personmgr.service.DomainService;

import static ru.majordomo.hms.personmgr.common.Constants.Exchanges.DOMAIN_CREATE;
import static ru.majordomo.hms.personmgr.common.Constants.Exchanges.DOMAIN_DELETE;
import static ru.majordomo.hms.personmgr.common.Constants.Exchanges.DOMAIN_UPDATE;
import static ru.majordomo.hms.personmgr.common.Constants.Exchanges.DOMAIN_TRANSFER_SYNCHRONIZATION;
import static ru.majordomo.hms.personmgr.common.Constants.*;

@Service
public class DomainAmqpController extends CommonAmqpController {
    private final CartManager cartManager;
    private final DomainService domainService;
    private final AccountStatHelper accountStatHelper;

    @Autowired
    public DomainAmqpController(
            CartManager cartManager,
            DomainService domainService,
            AccountStatHelper accountStatHelper
    ) {
        this.cartManager = cartManager;
        this.accountStatHelper = accountStatHelper;
        this.domainService = domainService;
        resourceName = UserConstants.DOMAIN;
    }

    @RabbitListener(queues = "${hms.instance.name}" + "." + "${spring.application.name}" + "." + DOMAIN_CREATE)
    public void create(Message amqpMessage, @Payload SimpleServiceMessage message, @Headers Map<String, String> headers) {
        String provider = headers.get("provider");
        logger.debug("Received from " + provider + ": " + message.toString());

        try {
            State state = businessFlowDirector.processMessage(message, resourceName);
            ProcessingBusinessAction businessAction =
                    processingBusinessActionRepository.findById(message.getActionIdentity()).orElse(null);

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
            String operationId = message.getOperationIdentity();
            ProcessingBusinessOperation operation = operationId == null ? null :
                    processingBusinessOperationRepository.findById(operationId).orElse(null);
            if (operation != null && operation.getType() == BusinessOperationType.SWITCH_ACCOUNT_RESOURCES) {
                handleUpdateEventFromRc(message, headers);
                return;
            }
            //todo поместить логику того что ниже в handleUpdateEventFromRc

            State state = businessFlowDirector.processMessage(message, resourceName);

            if (state != State.PROCESSED) {
                logger.error("State is not PROCESSED" );
                return;
            }

            ProcessingBusinessAction businessAction = processingBusinessActionRepository
                    .findById(message.getActionIdentity()).orElse(null);

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

    @RabbitListener(queues = "${hms.instance.name}" + "." + "${spring.application.name}" + "." + DOMAIN_TRANSFER_SYNCHRONIZATION)
    public void transferSynchronization(Message amqpMessage, @Payload DomainTransferSynchronization response, @Headers Map<String, String> headers) {
        logger.debug("Received domain transfer synchronization message: " + response.toString());

        if (!response.isValid()) {
            logger.error("Синхронизация домена после трансфера не выполнена: " + response.getValidationError());
            return;
        }

        try {
            if (response.isTransferAccepted()) {
                domainService.processSuccessfulTransfer(response.getDomainName());
            } else if (response.isTransferRejected()) {
                domainService.processRejectedTransfer(response.getDomainName());
            } else if (response.isTransferCancelled()) {
                domainService.processCancelledTransfer(response.getDomainName());
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Got Exception in DomainAmqpController.transferSynchronization: " + e.getMessage());
        }
    }
}
