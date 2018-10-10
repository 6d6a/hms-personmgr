package ru.majordomo.hms.personmgr.controller.amqp;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.Map;

import ru.majordomo.hms.personmgr.common.BusinessActionType;
import ru.majordomo.hms.personmgr.common.BusinessOperationType;
import ru.majordomo.hms.personmgr.common.State;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.manager.AbonementManager;
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

@Service
public class AccountAmqpController extends CommonAmqpController {
    private final BusinessHelper businessHelper;
    private final PromocodeService promocodeService;
    private final AbonementService abonementService;
    private final AbonementManager<AccountAbonement> accountAbonementManager;
    private final PromotionRepository promotionRepository;
    private final AccountHelper accountHelper;

    @Autowired
    public AccountAmqpController(
            BusinessHelper businessHelper,
            PromocodeService promocodeService,
            AbonementService abonementService,
            AbonementManager<AccountAbonement> accountAbonementManager,
            PromotionRepository promotionRepository,
            AccountHelper accountHelper) {
        this.businessHelper = businessHelper;
        this.promocodeService = promocodeService;
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
                            PersonalAccount account = accountManager.findOne(message.getAccountId());

                            if (businessOperation.getParam("promocode") != null) {
                                promocodeService.processPromocode(account, businessOperation.getParam("promocode").toString());
                            }

                            //Пробный период 14 дней - начисляем бонусный абонемент
                            AccountAbonement accountAbonement = accountAbonementManager.findByPersonalAccountId(account.getId());
                            if (accountAbonement == null) {
                                abonementService.addFree14DaysAbonement(account);
                            }

                            addPromoAfterRegistered(account);

                            if (businessOperation.getType() == BusinessOperationType.ACCOUNT_CREATE) {
                                message.setParams(businessOperation.getParams());
                                message.addParam("quota", (Long) businessOperation.getParams().get("quota") * 1024);
                                businessHelper.buildAction(BusinessActionType.UNIX_ACCOUNT_CREATE_RC, message);
                                history.save(account, "Заявка на первичное создание UNIX-аккаунта отправлена (имя: " + message.getParam("name") + ")");
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

    private void addPromoAfterRegistered(PersonalAccount account) {
        //Три домена RU и РФ по 49 рублей
        Promotion promotion = promotionRepository.findByName(DOMAIN_DISCOUNT_RU_RF);
        for (int i = 1; i <= DOMAIN_DISCOUNT_RU_RF_REGISTRATION_FREE_COUNT; i++) {
            accountHelper.giveGift(account, promotion);
        }
    }
}
