package ru.majordomo.hms.personmgr.controller.amqp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import ru.majordomo.hms.personmgr.repository.PersonalAccountRepository;
import ru.majordomo.hms.personmgr.repository.ProcessingBusinessOperationRepository;
import ru.majordomo.hms.personmgr.service.*;

@EnableRabbit
@Service
public class AccountAmqpController {

    private final static Logger logger = LoggerFactory.getLogger(AccountAmqpController.class);

    private final BusinessFlowDirector businessFlowDirector;
    private final BusinessActionBuilder businessActionBuilder;
    private final PromocodeProcessor promocodeProcessor;
    private final PersonalAccountRepository accountRepository;
    private final ProcessingBusinessOperationRepository processingBusinessOperationRepository;
    private final AbonementService abonementService;

    @Autowired
    public AccountAmqpController(
            BusinessFlowDirector businessFlowDirector,
            BusinessActionBuilder businessActionBuilder,
            PromocodeProcessor promocodeProcessor,
            PersonalAccountRepository accountRepository,
            ProcessingBusinessOperationRepository processingBusinessOperationRepository,
            AbonementService abonementService) {
        this.businessFlowDirector = businessFlowDirector;
        this.businessActionBuilder = businessActionBuilder;
        this.promocodeProcessor = promocodeProcessor;
        this.accountRepository = accountRepository;
        this.processingBusinessOperationRepository = processingBusinessOperationRepository;
        this.abonementService = abonementService;
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

                            // TODO проврка на наличии абонментов после обработки промокода
                            //Пробный период 14 дней - начисляем бонусный абонемент
                            PersonalAccount account = accountRepository.findOne(message.getAccountId());
                            if (account != null) {
                                abonementService.addFree14DaysAbonement(account);
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
