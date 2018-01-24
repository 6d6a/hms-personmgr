package ru.majordomo.hms.personmgr.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ru.majordomo.hms.personmgr.common.BusinessOperationType;
import ru.majordomo.hms.personmgr.common.State;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.manager.AccountPromotionManager;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessAction;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessOperation;
import ru.majordomo.hms.personmgr.repository.ProcessingBusinessActionRepository;
import ru.majordomo.hms.personmgr.repository.ProcessingBusinessOperationRepository;

import static ru.majordomo.hms.personmgr.common.Constants.BONUS_FREE_DOMAIN_PROMOCODE_ACTION_ID;
import static ru.majordomo.hms.personmgr.common.Constants.DOMAIN_DISCOUNT_RU_RF_ACTION_ID;

@Service
public class BusinessFlowDirector {
    private static final Logger logger = LoggerFactory.getLogger(BusinessFlowDirector.class);
    private final ProcessingBusinessActionRepository processingBusinessActionRepository;
    private final ProcessingBusinessOperationRepository processingBusinessOperationRepository;
    private final BusinessActionProcessor businessActionProcessor;
    private final FinFeignClient finFeignClient;
    private final AccountPromotionManager accountPromotionManager;

    @Autowired
    public BusinessFlowDirector(
            ProcessingBusinessActionRepository processingBusinessActionRepository,
            ProcessingBusinessOperationRepository processingBusinessOperationRepository,
            BusinessActionProcessor businessActionProcessor,
            FinFeignClient finFeignClient,
            AccountPromotionManager accountPromotionManager
    ) {
        this.processingBusinessActionRepository = processingBusinessActionRepository;
        this.processingBusinessOperationRepository = processingBusinessOperationRepository;
        this.businessActionProcessor = businessActionProcessor;
        this.finFeignClient = finFeignClient;
        this.accountPromotionManager = accountPromotionManager;
    }

    public void processClean(ProcessingBusinessAction businessAction) {
        logger.debug("Processing businessAction clean for " + businessAction.toString());

        switch (businessAction.getState()) {
            case ERROR:
                logger.debug("Found error businessAction " + businessAction.toString()
                );
            case PROCESSING:
            case PROCESSED:
            case FINISHED:
                processingBusinessActionRepository.delete(businessAction);

                break;
        }
    }

    public void process(ProcessingBusinessAction action) {
        action.setState(State.PROCESSING);

        processingBusinessActionRepository.save(action);

        businessActionProcessor.process(action);

        processingBusinessActionRepository.save(action);
    }

    public State processMessage(SimpleServiceMessage message) {
        logger.debug("Processing message : " + message.toString());

        ProcessingBusinessAction businessAction = processingBusinessActionRepository.findOne(message.getActionIdentity());

        if (businessAction != null) {
            if ((boolean) message.getParam("success")) {
                businessAction.setState(State.PROCESSED);
            } else {
                businessAction.setState(State.ERROR);
            }

            logger.debug("ProcessingBusinessAction -> " + businessAction.getState() + ", operationIdentity: " +
                    message.getOperationIdentity() +
                    " actionIdentity: " + message.getActionIdentity()
            );

            processingBusinessActionRepository.save(businessAction);

            processBlockedPayment(businessAction);

            if (businessAction.getOperationId() != null) {
                ProcessingBusinessOperation businessOperation = processingBusinessOperationRepository.findOne(businessAction.getOperationId());
                if (businessOperation != null) {
                    switch (businessAction.getState()) {
                        case PROCESSED:
                            if (businessOperation.getType() != BusinessOperationType.ACCOUNT_CREATE
                                    && businessOperation.getType() != BusinessOperationType.APP_INSTALL
                                    && businessOperation.getType() != BusinessOperationType.ACCOUNT_TRANSFER
                                    && businessOperation.getType() != BusinessOperationType.APP_INSTALL) {
                                businessOperation.setState(businessAction.getState());
                            }

                            break;
                        case ERROR:
                            businessOperation.setState(businessAction.getState());
                            if (message.getParam("errorMessage") != null && !message.getParam("errorMessage").equals(""))
                                businessOperation.addPublicParam("message", message.getParam("errorMessage"));
                    }
                    logger.debug("ProcessingBusinessOperation -> " + businessOperation.getState() + ", operationIdentity: " +
                            message.getOperationIdentity()
                    );
                    processingBusinessOperationRepository.save(businessOperation);
                }
            }

            if (businessAction.getState() == State.ERROR) {
                if (businessAction.getMessage().getParam("freeDomainPromotionId") != null) {
                    accountPromotionManager.activateAccountPromotionByIdAndActionId(
                            (String) businessAction.getMessage().getParam("freeDomainPromotionId"),
                            BONUS_FREE_DOMAIN_PROMOCODE_ACTION_ID
                    );
                }

                if (businessAction.getMessage().getParam("domainDiscountPromotionId") != null) {
                    accountPromotionManager.activateAccountPromotionByIdAndActionId(
                            (String) businessAction.getMessage().getParam("domainDiscountPromotionId"),
                            DOMAIN_DISCOUNT_RU_RF_ACTION_ID
                    );
                }
            }

            return businessAction.getState();
        } else {
            logger.debug("ProcessingBusinessAction with id: " + message.getActionIdentity() + " not found");
            return State.ERROR;
        }
    }

    private void processBlockedPayment(ProcessingBusinessAction businessAction) {
        if (businessAction.getState() == State.PROCESSED && businessAction.getMessage().getParam("documentNumber") != null) {
            //Спишем заблокированные средства
            try {
                finFeignClient.chargeBlocked(businessAction.getMessage().getAccountId(), (String) businessAction.getMessage().getParam("documentNumber"));
            } catch (Exception e) {
                e.printStackTrace();
                logger.error("Exception in ru.majordomo.hms.personmgr.service.BusinessFlowDirector.processBlockedPayment #1 " + e.getMessage());
            }
        } else if (businessAction.getState() == State.ERROR && businessAction.getMessage().getParam("documentNumber") != null) {
            //Разблокируем средства
            try {
                finFeignClient.unblock(businessAction.getMessage().getAccountId(), (String) businessAction.getMessage().getParam("documentNumber"));
            } catch (Exception e) {
                e.printStackTrace();
                logger.error("Exception in ru.majordomo.hms.personmgr.service.BusinessFlowDirector.processBlockedPayment #2 " + e.getMessage());
            }
        }
    }
}
