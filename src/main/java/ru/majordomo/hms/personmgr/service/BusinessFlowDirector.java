package ru.majordomo.hms.personmgr.service;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.collections.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.majordomo.hms.personmgr.common.BusinessOperationType;
import ru.majordomo.hms.personmgr.common.Constants;
import ru.majordomo.hms.personmgr.common.State;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.controller.amqp.CommonAmqpController;
import ru.majordomo.hms.personmgr.feign.FinFeignClient;
import ru.majordomo.hms.personmgr.importing.DBImportService;
import ru.majordomo.hms.personmgr.manager.AccountPromotionManager;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessAction;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessOperation;
import ru.majordomo.hms.personmgr.repository.ProcessingBusinessActionRepository;
import ru.majordomo.hms.personmgr.repository.ProcessingBusinessOperationRepository;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static ru.majordomo.hms.personmgr.common.Utils.cleanBooleanSafe;

@Service
@RequiredArgsConstructor
public class BusinessFlowDirector {
    private static final Logger logger = LoggerFactory.getLogger(BusinessFlowDirector.class);
    private final ProcessingBusinessActionRepository processingBusinessActionRepository;
    private final ProcessingBusinessOperationRepository processingBusinessOperationRepository;
    private final BusinessActionProcessor businessActionProcessor;
    private final FinFeignClient finFeignClient;
    private final AccountPromotionManager accountPromotionManager;
    private final ResourceHelper resourceHelper;

    @Setter
    @Nullable
    @Autowired(required = false)
    private DBImportService dbImportService;

    public void processClean(ProcessingBusinessAction businessAction) {
        logger.debug("Processing businessAction clean for " + businessAction.toString());

        switch (businessAction.getState()) {
            case ERROR:
                logger.debug("Found error businessAction " + businessAction.toString());
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

    /**
     * списывает, восстанавливает деньги, выставляет статус, завершает ProcessingBusinessAction и ProcessingBusinessOperation
     * @param resourceName {@link CommonAmqpController#getResourceName()} например "сайт"
     * @return статус ProcessingBusinessAction
     */
    public State processMessage(SimpleServiceMessage message, @Nonnull String resourceName) {
        logger.debug("Processing message : " + message.toString());

        ProcessingBusinessAction businessAction = message.getActionIdentity() == null ? null :
                processingBusinessActionRepository.findById(message.getActionIdentity()).orElse(null);

        if (businessAction != null) {
            if (cleanBooleanSafe(message.getParam("success"))) {
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
                ProcessingBusinessOperation businessOperation =
                        processingBusinessOperationRepository
                                .findById(businessAction.getOperationId())
                                .orElse(null);

                if (businessOperation != null) {
                    switch (businessAction.getState()) {
                        case PROCESSED:
                            switch (businessOperation.getType()) {
                                case ACCOUNT_CREATE:
                                case APP_INSTALL:
                                case ACCOUNT_TRANSFER:
                                case WEB_SITE_UPDATE_EXTENDED_ACTION:
                                case IMPORT_FROM_BILLINGDB:
                                case SWITCH_ACCOUNT_RESOURCES:
                                    break;
                                default:
                                    businessOperation.setState(businessAction.getState());
                                    processingBusinessOperationRepository.save(businessOperation);
                                    break;
                            }
                            break;
                        case ERROR:
                            if (dbImportService != null && businessOperation.getType() == BusinessOperationType.IMPORT_FROM_BILLINGDB) {
                                dbImportService.processErrorAction(businessAction, businessOperation, message);
                            } else if (businessOperation.getType() == BusinessOperationType.SWITCH_ACCOUNT_RESOURCES) {
                                resourceHelper.processErrorActionForSwitchAccountResources(businessAction, businessOperation, message, resourceName);
                            } else {
                                businessOperation.setState(businessAction.getState());
                                fillPublicParamsToBusinessOperation(message, businessOperation);
                                processingBusinessOperationRepository.save(businessOperation);
                            }
                            break;
                    }
                    logger.debug("ProcessingBusinessOperation -> " + businessOperation.getState() + ", operationIdentity: " +
                            message.getOperationIdentity()
                    );
                }
            }

            if (businessAction.getState() == State.ERROR) {
                if (businessAction.getMessage().getParam("accountPromotionId") != null) {
                    accountPromotionManager.setAsActiveAccountPromotionById((String) businessAction.getMessage().getParam("accountPromotionId"));
                }
            }

            return businessAction.getState();
        } else {
            logger.debug("ProcessingBusinessAction with id: " + message.getActionIdentity() + " not found");
            return State.ERROR;
        }
    }

    private void processBlockedPayment(ProcessingBusinessAction businessAction) {
        if (businessAction.getState() == State.PROCESSED && businessAction.getMessage().getParam(Constants.DOCUMENT_NUMBER_KEY) != null) {
            //Спишем заблокированные средства
            try {
                finFeignClient.chargeBlocked(businessAction.getMessage().getAccountId(), (String) businessAction.getMessage().getParam(Constants.DOCUMENT_NUMBER_KEY));
            } catch (Exception e) {
                e.printStackTrace();
                logger.error("Exception in ru.majordomo.hms.personmgr.service.BusinessFlowDirector.processBlockedPayment #1 " + e.getMessage());
            }
        } else if (businessAction.getState() == State.ERROR && businessAction.getMessage().getParam(Constants.DOCUMENT_NUMBER_KEY) != null) {
            //Разблокируем средства
            try {
                finFeignClient.unblock(businessAction.getMessage().getAccountId(), (String) businessAction.getMessage().getParam(Constants.DOCUMENT_NUMBER_KEY));
            } catch (Exception e) {
                e.printStackTrace();
                logger.error("Exception in ru.majordomo.hms.personmgr.service.BusinessFlowDirector.processBlockedPayment #2 " + e.getMessage());
            }
        }
    }

    private void fillPublicParamsToBusinessOperation(SimpleServiceMessage message, ProcessingBusinessOperation businessOperation) {
        try {
            String errorMessage = MapUtils.getString(message.getParams(), Constants.ERROR_MESSAGE_KEY, "");
            String bigErrorMessage = MapUtils.getString(message.getParams(), "bigErrorMessage", "");
            if (!errorMessage.isEmpty()) {
                businessOperation.addPublicParam(Constants.MESSAGE_KEY, errorMessage);
            }
            if (!bigErrorMessage.isEmpty()) {
                //todo remove bigErrorMessage
                businessOperation.addParam("bigErrorMessage", bigErrorMessage);
                businessOperation.addPublicParam("isBigErrorMessage", true);
            }
            if (message.getParam("errors") != null) {
                businessOperation.addPublicParam("errors", message.getParam("errors"));
            }
            if (message.getParam("exceptionClass") != null) {
                businessOperation.addPublicParam("exceptionClass", message.getParam("exceptionClass"));
            }

            switch (businessOperation.getType()) {
                case SSL_CERTIFICATE_CREATE:
                case SSL_CERTIFICATE_UPDATE:
                    if (message.getParam("isSafeBrowsing") != null)
                        businessOperation.addPublicParam("isSafeBrowsing", message.getParam("isSafeBrowsing"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Catch exception in fillPublicParams, message: " + e.getMessage());
        }
    }
}
