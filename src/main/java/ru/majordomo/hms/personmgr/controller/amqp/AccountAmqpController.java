package ru.majordomo.hms.personmgr.controller.amqp;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

import ru.majordomo.hms.personmgr.common.BusinessActionType;
import ru.majordomo.hms.personmgr.common.BusinessOperationType;
import ru.majordomo.hms.personmgr.common.State;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.event.accountHistory.AccountHistoryEvent;
import ru.majordomo.hms.personmgr.manager.AccountAbonementManager;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessOperation;
import ru.majordomo.hms.personmgr.model.abonement.AccountAbonement;
import ru.majordomo.hms.personmgr.model.promotion.Promotion;
import ru.majordomo.hms.personmgr.repository.PromotionRepository;
import ru.majordomo.hms.personmgr.service.*;

import static ru.majordomo.hms.personmgr.common.Constants.DOMAIN_DISCOUNT_RU_RF;
import static ru.majordomo.hms.personmgr.common.Constants.DOMAIN_DISCOUNT_RU_RF_REGISTRATION_FREE_COUNT;
import static ru.majordomo.hms.personmgr.common.Constants.Exchanges.ACCOUNT_CREATE;
import static ru.majordomo.hms.personmgr.common.Constants.Exchanges.ACCOUNT_DELETE;
import static ru.majordomo.hms.personmgr.common.Constants.Exchanges.ACCOUNT_UPDATE;
import static ru.majordomo.hms.personmgr.common.Constants.HISTORY_MESSAGE_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.OPERATOR_KEY;

@Service
public class AccountAmqpController extends CommonAmqpController {
    private final BusinessHelper businessHelper;
    private final PromocodeProcessor promocodeProcessor;
    private final AbonementService abonementService;
    private final AccountAbonementManager accountAbonementManager;
    private final PromotionRepository promotionRepository;
    private final AccountHelper accountHelper;

    @Autowired
    public AccountAmqpController(
            BusinessHelper businessHelper,
            PromocodeProcessor promocodeProcessor,
            AbonementService abonementService,
            AccountAbonementManager accountAbonementManager,
            PromotionRepository promotionRepository,
            AccountHelper accountHelper) {
        this.businessHelper = businessHelper;
        this.promocodeProcessor = promocodeProcessor;
        this.abonementService = abonementService;
        this.accountAbonementManager = accountAbonementManager;
        this.promotionRepository = promotionRepository;
        this.accountHelper = accountHelper;
        resourceName = "аккаунт";
    }

    @RabbitListener(queues = "${hms.instance.name}" + "." + "${spring.application.name}" + "." + ACCOUNT_CREATE)
    public void create(Message amqpMessage, @Payload SimpleServiceMessage message, @Headers Map<String, String> headers) {
        String provider = headers.get("provider");
        logger.debug("Received from " + provider + ": " + message.toString());

        String realProviderName = provider.replaceAll("^" + instanceName + "\\.", "");

        try {
            State state = businessFlowDirector.processMessage(message);

            ProcessingBusinessOperation businessOperation = processingBusinessOperationRepository.findOne(message.getOperationIdentity());

            switch (realProviderName) {
                case "si":
                    if (state == State.PROCESSED) {
//                        if (businessOperation != null && businessOperation.getType() == BusinessOperationType.ACCOUNT_CREATE) {
//                            message.setParams(businessOperation.getParams());
//                            businessActionBuilder.build(BusinessActionType.ACCOUNT_CREATE_FIN, message);
//                        }
                    }
                    break;
                case "fin":
                    if (state == State.PROCESSED) {
                        if (businessOperation != null) {
                            //надо обработать промокод
                            if (businessOperation.getParam("promocode") != null) {
                                PersonalAccount account = accountManager.findOne(message.getAccountId());
                                if (account != null) {
                                    logger.debug("We got promocode " + businessOperation.getParam("promocode") + ". Try to process it");
                                    promocodeProcessor.processPromocode(account, (String) businessOperation.getParam("promocode"));
                                }
                            }

                            PersonalAccount account = accountManager.findOne(message.getAccountId());
                            if (account != null) {

                                //Пробный период 14 дней - начисляем бонусный абонемент
                                AccountAbonement accountAbonement = accountAbonementManager.findByPersonalAccountId(account.getId());
                                if (accountAbonement == null) {
                                    abonementService.addFree14DaysAbonement(account);
                                }

                                //Три домена RU и РФ по 49 рублей
                                Promotion promotion = promotionRepository.findByName(DOMAIN_DISCOUNT_RU_RF);
                                for (int i = 1; i <= DOMAIN_DISCOUNT_RU_RF_REGISTRATION_FREE_COUNT; i++) {
                                    accountHelper.giveGift(account, promotion);
                                }

                            }

                            if (businessOperation.getType() == BusinessOperationType.ACCOUNT_CREATE) {
                                message.setParams(businessOperation.getParams());
                                message.addParam("quota", (Long) businessOperation.getParams().get("quota") * 1024);
                                businessHelper.buildAction(BusinessActionType.UNIX_ACCOUNT_CREATE_RC, message);

                                try {
                                    //Save history
                                    Map<String, String> params = new HashMap<>();
                                    params.put(HISTORY_MESSAGE_KEY, "Заявка на первичное создание UNIX-аккаунта отправлена (имя: " + message.getParam("name") + ")");
                                    params.put(OPERATOR_KEY, "service");

                                    publisher.publishEvent(new AccountHistoryEvent(message.getAccountId(), params));
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    logger.error("Got Exception in AccountAmqpController.create " + e.getMessage());
                                }
                            }
                        }
                    } else {
                        //TODO delete si account and PM
                    }
                    break;
            }

            saveLogByMessageStateForCreate(message, state);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Got Exception in AccountAmqpController.create " + e.getMessage());
        }
    }

    @RabbitListener(queues = "${hms.instance.name}" + "." + "${spring.application.name}" + "." + ACCOUNT_UPDATE)
    public void update(Message amqpMessage, @Payload SimpleServiceMessage message, @Headers Map<String, String> headers) {
        String provider = headers.get("provider");
        logger.debug("Received update message from " + provider + ": " + message.toString());

        State state = businessFlowDirector.processMessage(message);
    }

    @RabbitListener(queues = "${hms.instance.name}" + "." + "${spring.application.name}" + "." + ACCOUNT_DELETE)
    public void delete(Message amqpMessage, @Payload SimpleServiceMessage message, @Headers Map<String, String> headers) {
        String provider = headers.get("provider");
        logger.debug("Received delete message from " + provider + ": " + message.toString());

        State state = businessFlowDirector.processMessage(message);
    }
}
