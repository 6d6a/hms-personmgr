package ru.majordomo.hms.personmgr.service;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import ru.majordomo.hms.personmgr.common.BusinessActionType;
import ru.majordomo.hms.personmgr.common.BusinessOperationType;
import ru.majordomo.hms.personmgr.common.State;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.event.processingBusinessAction.ProcessingBusinessActionNewEvent;
import ru.majordomo.hms.personmgr.exception.BusinessActionNotFoundException;
import ru.majordomo.hms.personmgr.model.business.BusinessAction;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessAction;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessOperation;
import ru.majordomo.hms.personmgr.repository.BusinessActionRepository;
import ru.majordomo.hms.personmgr.repository.ProcessingBusinessActionRepository;
import ru.majordomo.hms.personmgr.repository.ProcessingBusinessOperationRepository;

@Service
public class BusinessHelper {
    private final ProcessingBusinessOperationRepository operationRepository;
    private final BusinessActionRepository businessActionRepository;
    private final ProcessingBusinessActionRepository processingBusinessActionRepository;
    private final ApplicationEventPublisher publisher;

    public BusinessHelper(
            ProcessingBusinessOperationRepository operationRepository,
            BusinessActionRepository businessActionRepository,
            ProcessingBusinessActionRepository processingBusinessActionRepository,
            ApplicationEventPublisher publisher
    ) {
        this.operationRepository = operationRepository;
        this.businessActionRepository = businessActionRepository;
        this.processingBusinessActionRepository = processingBusinessActionRepository;
        this.publisher = publisher;
    }

    public ProcessingBusinessAction buildActionAndOperation(
            BusinessOperationType operationType,
            BusinessActionType actionType,
            SimpleServiceMessage message
    ) {
        ProcessingBusinessOperation processingBusinessOperation = buildOperation(operationType, message);

        return buildActionByOperation(actionType, message, processingBusinessOperation);
    }

    public ProcessingBusinessOperation buildOperation(BusinessOperationType operationType, SimpleServiceMessage message) {
        ProcessingBusinessOperation operation = new ProcessingBusinessOperation();

        operation.setPersonalAccountId(message.getAccountId());
        operation.setState(State.PROCESSING);
        operation.setParams(message.getParams());
        operation.setType(operationType);

        String nameInParams = (String) message.getParam("name");
        if (nameInParams != null) {
            operation.addPublicParam("name", nameInParams);
        }

        operationRepository.save(operation);

        return operation;
    }

    public ProcessingBusinessAction buildAction(
            BusinessActionType businessActionType,
            SimpleServiceMessage message
    ) {
        BusinessAction businessAction = businessActionRepository.findByBusinessActionType(businessActionType);

        ProcessingBusinessAction processingBusinessAction;

        if (businessAction != null) {
            processingBusinessAction = new ProcessingBusinessAction(businessAction);

            processingBusinessAction.setMessage(message);
            processingBusinessAction.setOperationId(message.getOperationIdentity());
            processingBusinessAction.setParams(message.getParams());
            processingBusinessAction.setState(State.NEED_TO_PROCESS);
            processingBusinessAction.setPersonalAccountId(message.getAccountId());
        } else {
            throw new BusinessActionNotFoundException();
        }
        processingBusinessActionRepository.save(processingBusinessAction);

        publisher.publishEvent(new ProcessingBusinessActionNewEvent(processingBusinessAction));

        return processingBusinessAction;
    }

    public ProcessingBusinessAction buildActionByOperation(
            BusinessActionType businessActionType,
            SimpleServiceMessage message,
            ProcessingBusinessOperation operation
    ) {
        return buildActionByOperationId(businessActionType, message, operation.getId());
    }

    public ProcessingBusinessAction buildActionByOperationId(
            BusinessActionType businessActionType,
            SimpleServiceMessage message,
            String operationId
    ) {
        ProcessingBusinessAction action = buildAction(businessActionType, message);
        action.setOperationId(operationId);

        processingBusinessActionRepository.save(action);

        return action;
    }
}
