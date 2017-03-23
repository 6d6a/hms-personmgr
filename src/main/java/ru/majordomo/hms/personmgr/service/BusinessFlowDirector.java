package ru.majordomo.hms.personmgr.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ru.majordomo.hms.personmgr.Application;
import ru.majordomo.hms.personmgr.common.BusinessOperationType;
import ru.majordomo.hms.personmgr.common.State;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.model.ProcessingBusinessAction;
import ru.majordomo.hms.personmgr.model.ProcessingBusinessOperation;
import ru.majordomo.hms.personmgr.repository.ProcessingBusinessActionRepository;
import ru.majordomo.hms.personmgr.repository.ProcessingBusinessOperationRepository;

@Service
public class BusinessFlowDirector {
    private static final Logger logger = LoggerFactory.getLogger(Application.class);
    private final ProcessingBusinessActionRepository processingBusinessActionRepository;
    private final ProcessingBusinessOperationRepository processingBusinessOperationRepository;
    private final BusinessActionProcessor businessActionProcessor;
    private final FinFeignClient finFeignClient;

    @Autowired
    public BusinessFlowDirector(
            ProcessingBusinessActionRepository processingBusinessActionRepository,
            ProcessingBusinessOperationRepository processingBusinessOperationRepository,
            BusinessActionProcessor businessActionProcessor,
            FinFeignClient finFeignClient
    ) {
        this.processingBusinessActionRepository = processingBusinessActionRepository;
        this.processingBusinessOperationRepository = processingBusinessOperationRepository;
        this.businessActionProcessor = businessActionProcessor;
        this.finFeignClient = finFeignClient;
    }

    public void processClean(ProcessingBusinessAction businessAction) {
        logger.debug("Processing businessAction clean for " + businessAction.toString());

        logger.error("Found old businessAction with " + businessAction.getState() +
                " state " + businessAction.toString()
        );

        switch (businessAction.getState()) {
            case ERROR:
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
                            if (businessOperation.getType() != BusinessOperationType.ACCOUNT_CREATE) {
                                businessOperation.setState(businessAction.getState());
                            }
                        case ERROR:
                            businessOperation.setState(businessAction.getState());
                            if (message.getParam("errorMessage") != null && !message.getParam("errorMessage").equals(""))
                                businessOperation.addParam("message", message.getParam("errorMessage"));
                    }
                    logger.debug("ProcessingBusinessOperation -> " + businessOperation.getState() + ", operationIdentity: " +
                            message.getOperationIdentity()
                    );
                    processingBusinessOperationRepository.save(businessOperation);
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
            }
        } else if (businessAction.getState() == State.ERROR && businessAction.getMessage().getParam("documentNumber") != null) {
            //Разблокируем средства
            try {
                finFeignClient.unblock(businessAction.getMessage().getAccountId(), (String) businessAction.getMessage().getParam("documentNumber"));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
