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
import ru.majordomo.hms.personmgr.manager.PlanManager;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessOperation;
import ru.majordomo.hms.personmgr.model.abonement.AccountAbonement;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.service.*;

import static ru.majordomo.hms.personmgr.common.Constants.Exchanges.ACCOUNT_CREATE;
import static ru.majordomo.hms.personmgr.common.Constants.Exchanges.ACCOUNT_DELETE;
import static ru.majordomo.hms.personmgr.common.Constants.Exchanges.ACCOUNT_UPDATE;

@Service
public class AccountAmqpController extends CommonAmqpController {
    private final BusinessHelper businessHelper;
    private final PromocodeService promocodeService;
    private final AbonementService abonementService;
    private final AbonementManager<AccountAbonement> accountAbonementManager;
    private final PlanManager planManager;
    private final PlanLimitsService planLimitsService;

    @Autowired
    public AccountAmqpController(
            BusinessHelper businessHelper,
            PromocodeService promocodeService,
            AbonementService abonementService,
            AbonementManager<AccountAbonement> accountAbonementManager,
            PlanManager planManager,
            PlanLimitsService planLimitsService
    ) {
        this.businessHelper = businessHelper;
        this.promocodeService = promocodeService;
        this.abonementService = abonementService;
        this.accountAbonementManager = accountAbonementManager;
        this.planManager = planManager;
        this.planLimitsService = planLimitsService;
        resourceName = "аккаунт";
    }

    @RabbitListener(queues = "${hms.instance.name}" + "." + "${spring.application.name}" + "." + ACCOUNT_CREATE)
    public void create(Message amqpMessage, @Payload SimpleServiceMessage message, @Headers Map<String, String> headers) {
        String provider = headers.get("provider");
        logger.debug("Received from " + provider + ": " + message.toString());

        String realProviderName = provider.replaceAll("^" + instanceName + "\\.", "");

        try {
            State state = businessFlowDirector.processMessage(message);

            ProcessingBusinessOperation businessOperation =
                    processingBusinessOperationRepository
                            .findById(message.getOperationIdentity())
                            .orElse(null);

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
                            promocodeService.processRegistration(businessOperation);

                            PersonalAccount account = accountManager.findOne(message.getAccountId());

                            //Пробный период 14 дней - начисляем бонусный абонемент
                            AccountAbonement accountAbonement = accountAbonementManager.findByPersonalAccountId(account.getId());
                            if (accountAbonement == null) {
                                abonementService.addFree14DaysAbonement(account);
                            }

                            if (businessOperation.getType() == BusinessOperationType.ACCOUNT_CREATE) {
                                //После применения промокода может быть изменен тариф
                                Plan plan = planManager.findOne(account.getPlanId());
                                Long quota = planLimitsService.getQuotaBytesFreeLimit(plan);

                                message.setParams(businessOperation.getParams());
                                message.addParam("quota", quota);

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
}
