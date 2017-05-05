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

import java.util.Map;

import ru.majordomo.hms.personmgr.common.BusinessActionType;
import ru.majordomo.hms.personmgr.common.BusinessOperationType;
import ru.majordomo.hms.personmgr.common.State;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.model.PersonalAccount;
import ru.majordomo.hms.personmgr.model.ProcessingBusinessOperation;
import ru.majordomo.hms.personmgr.model.abonement.AccountAbonement;
import ru.majordomo.hms.personmgr.model.promotion.Promotion;
import ru.majordomo.hms.personmgr.repository.AccountAbonementRepository;
import ru.majordomo.hms.personmgr.repository.ProcessingBusinessOperationRepository;
import ru.majordomo.hms.personmgr.repository.PromotionRepository;
import ru.majordomo.hms.personmgr.service.*;

import static ru.majordomo.hms.personmgr.common.Constants.DOMAIN_DISCOUNT_RU_RF;
import static ru.majordomo.hms.personmgr.common.Constants.DOMAIN_DISCOUNT_RU_RF_REGISTRATION_FREE_COUNT;

@EnableRabbit
@Service
public class AccountAmqpController extends CommonAmqpController {
    private final BusinessActionBuilder businessActionBuilder;
    private final PromocodeProcessor promocodeProcessor;
    private final ProcessingBusinessOperationRepository processingBusinessOperationRepository;
    private final AbonementService abonementService;
    private final AccountAbonementRepository accountAbonementRepository;
    private final PromotionRepository promotionRepository;
    private final AccountHelper accountHelper;

    @Autowired
    public AccountAmqpController(
            BusinessActionBuilder businessActionBuilder,
            PromocodeProcessor promocodeProcessor,
            ProcessingBusinessOperationRepository processingBusinessOperationRepository,
            AbonementService abonementService,
            AccountAbonementRepository accountAbonementRepository,
            PromotionRepository promotionRepository,
            AccountHelper accountHelper) {
        this.businessActionBuilder = businessActionBuilder;
        this.promocodeProcessor = promocodeProcessor;
        this.processingBusinessOperationRepository = processingBusinessOperationRepository;
        this.abonementService = abonementService;
        this.accountAbonementRepository = accountAbonementRepository;
        this.promotionRepository = promotionRepository;
        this.accountHelper = accountHelper;
    }

    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = "pm.account.create",
                                                            durable = "true",
                                                            autoDelete = "false"),
                                             exchange = @Exchange(value = "account.create",
                                                                  type = ExchangeTypes.TOPIC),
                                             key = "pm"))
    public void create(@Payload SimpleServiceMessage message, @Headers Map<String, String> headers) {
        String provider = headers.get("provider");
        logger.debug("Received from " + provider + ": " + message.toString());

        try {
            State state = businessFlowDirector.processMessage(message);

            ProcessingBusinessOperation businessOperation = processingBusinessOperationRepository.findOne(message.getOperationIdentity());

            switch (provider) {
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
                                PersonalAccount account = accountRepository.findOne(message.getAccountId());
                                if (account != null) {
                                    logger.debug("We got promocode " + businessOperation.getParam("promocode") + ". Try to process it");
                                    promocodeProcessor.processPromocode(account, (String) businessOperation.getParam("promocode"));
                                }
                            }

                            PersonalAccount account = accountRepository.findOne(message.getAccountId());
                            if (account != null) {

                                //Пробный период 14 дней - начисляем бонусный абонемент
                                AccountAbonement accountAbonement = accountAbonementRepository.findByPersonalAccountIdAndPreordered(account.getId(), false);
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
                                //Создадим персону
                                businessActionBuilder.build(BusinessActionType.PERSON_CREATE_RC, message);
                            }
                        }
                    } else {
                        //TODO delete si account and PM
                    }
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = "pm.account.update",
                                                            durable = "true",
                                                            autoDelete = "false"),
                                             exchange = @Exchange(value = "account.update",
                                                                  type = ExchangeTypes.TOPIC),
                                             key = "pm"))
    public void update(@Payload SimpleServiceMessage message, @Headers Map<String, String> headers) {
        String provider = headers.get("provider");
        logger.debug("Received update message from " + provider + ": " + message.toString());

        State state = businessFlowDirector.processMessage(message);
    }

    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = "pm.account.delete",
                                                            durable = "true",
                                                            autoDelete = "false"),
                                             exchange = @Exchange(value = "account.delete",
                                                                  type = ExchangeTypes.TOPIC),
                                             key = "pm"))
    public void delete(@Payload SimpleServiceMessage message, @Headers Map<String, String> headers) {
        String provider = headers.get("provider");
        logger.debug("Received delete message from " + provider + ": " + message.toString());

        State state = businessFlowDirector.processMessage(message);
    }
}
