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

import java.util.HashMap;
import java.util.Map;

import ru.majordomo.hms.personmgr.common.BusinessActionType;
import ru.majordomo.hms.personmgr.common.State;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.model.PersonalAccount;
import ru.majordomo.hms.personmgr.model.ProcessingBusinessAction;
import ru.majordomo.hms.personmgr.model.ProcessingBusinessOperation;
import ru.majordomo.hms.personmgr.repository.PersonalAccountRepository;
import ru.majordomo.hms.personmgr.repository.ProcessingBusinessActionRepository;
import ru.majordomo.hms.personmgr.repository.ProcessingBusinessOperationRepository;
import ru.majordomo.hms.personmgr.service.BusinessActionBuilder;
import ru.majordomo.hms.personmgr.service.BusinessFlowDirector;
import ru.majordomo.hms.personmgr.service.PromocodeProcessor;

@EnableRabbit
@Service
public class AmqpAccountController {

    private final static Logger logger = LoggerFactory.getLogger(AmqpAccountController.class);

    @Autowired
    private BusinessFlowDirector businessFlowDirector;
    @Autowired
    private BusinessActionBuilder businessActionBuilder;

    @Autowired
    private ProcessingBusinessActionRepository processingBusinessActionRepository;

    @Autowired
    private ProcessingBusinessOperationRepository processingBusinessOperationRepository;

    @Autowired
    private PromocodeProcessor promocodeProcessor;

    @Autowired
    private PersonalAccountRepository personalAccountRepository;

    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = "pm.account.create", durable = "true", autoDelete = "true"), exchange = @Exchange(value = "account.create", type = ExchangeTypes.TOPIC), key = "pm"))
    public void create(@Payload SimpleServiceMessage message, @Headers Map<String, String> headers) {
        String provider = headers.get("provider");
        logger.info("Received from " + provider + ": " + message.toString());

        try {
            State state = businessFlowDirector.processMessage(message);

            switch (provider) {
                case "si":
                    if (state == State.PROCESSED) {
                        ProcessingBusinessAction processingBusinessAction = businessActionBuilder.build(BusinessActionType.ACCOUNT_CREATE_FIN, message);

//                        processingBusinessActionRepository.save(processingBusinessAction);
                    }
                    break;
                case "fin":
                    if (state == State.PROCESSED) {
                        ProcessingBusinessAction businessAction = businessActionBuilder.build(BusinessActionType.ACCOUNT_CREATE_RC, message);

//                        processingBusinessActionRepository.save(businessAction);
                    } else {
                        //TODO delete si account and PM
                    }
                    break;
                case "rc-user":
                    if (state == State.PROCESSED) {
                        ProcessingBusinessOperation businessOperation = processingBusinessOperationRepository.findOne(message.getOperationIdentity());
                        if (businessOperation != null) {

                            PersonalAccount personalAccount = personalAccountRepository.findOne(message.getAccountId());
                            //надо обработать промокод
                            if (personalAccount != null && message.getParam("promocode") != null) {
                                logger.info("We got promocode " + message.getParam("promocode") + ". Try to process it");
                                promocodeProcessor.processPromocode(personalAccount, (String) message.getParam("promocode"));
                            }

                            businessOperation.setState(State.PROCESSED);
                            processingBusinessOperationRepository.save(businessOperation);

                            String email = (String) message.getParam("email");
                            message.setParams(new HashMap<>());
                            message.addParam("email", email);
                            message.addParam("api_name", "MajordomoVHClientCreatedConfirmation");
                            message.addParam("priority", 10);

                            HashMap<String, String> parameters = new HashMap<>();
                            parameters.put("client_id", message.getAccountId());
                            parameters.put("password", (String) businessOperation.getMapParam("password"));
                            parameters.put("ftp_ip", "ftp_ip");
                            parameters.put("ftp_login", "ftp_login");
                            parameters.put("ftp_password", "ftp_password");

                            message.addParam("parametrs", parameters);

                            ProcessingBusinessAction businessAction = businessActionBuilder.build(BusinessActionType.WEB_SITE_CREATE_MM, message);

//                            processingBusinessActionRepository.save(businessAction);
                        }
                    }
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = "pm.account.update", durable = "true", autoDelete = "true"), exchange = @Exchange(value = "account.update", type = ExchangeTypes.TOPIC), key = "pm"))
    public void update(@Payload SimpleServiceMessage message, @Headers Map<String, String> headers) {
        String provider = headers.get("provider");
        logger.info("Received update message from " + provider + ": " + message.toString());

        State state = businessFlowDirector.processMessage(message);
    }

    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = "pm.account.delete", durable = "true", autoDelete = "true"), exchange = @Exchange(value = "account.delete", type = ExchangeTypes.TOPIC), key = "pm"))
    public void delete(@Payload SimpleServiceMessage message, @Headers Map<String, String> headers) {
        String provider = headers.get("provider");
        logger.info("Received delete message from " + provider + ": " + message.toString());

        State state = businessFlowDirector.processMessage(message);
    }
}
