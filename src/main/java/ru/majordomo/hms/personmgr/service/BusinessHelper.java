package ru.majordomo.hms.personmgr.service;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import ru.majordomo.hms.personmgr.common.BusinessActionType;
import ru.majordomo.hms.personmgr.common.BusinessOperationType;
import ru.majordomo.hms.personmgr.common.State;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.event.processingBusinessAction.ProcessingBusinessActionNewEvent;
import ru.majordomo.hms.personmgr.exception.ResourceNotFoundException;
import ru.majordomo.hms.personmgr.model.business.BusinessAction;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessAction;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessOperation;
import ru.majordomo.hms.personmgr.repository.BusinessActionRepository;
import ru.majordomo.hms.personmgr.repository.ProcessingBusinessActionRepository;
import ru.majordomo.hms.personmgr.repository.ProcessingBusinessOperationRepository;

import java.util.*;

import static ru.majordomo.hms.personmgr.common.BusinessOperationType.BUSINESS_OPERATION_TYPE2HUMAN;
import static ru.majordomo.hms.personmgr.common.Constants.ARCHIVED_RESOURCE_ID_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.RESOURCE_TYPE;
import static ru.majordomo.hms.personmgr.common.State.NEED_TO_PROCESS;
import static ru.majordomo.hms.personmgr.common.State.PROCESSING;

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
        operation.setName(BUSINESS_OPERATION_TYPE2HUMAN.get(operationType));
        operation.setState(PROCESSING);
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

        if (businessAction == null) {
            throw new ResourceNotFoundException("BusinessAction with type " + businessActionType + " not found");
        }

        ProcessingBusinessAction processingBusinessAction = new ProcessingBusinessAction(businessAction);

        processingBusinessAction.setMessage(message);
        processingBusinessAction.setOperationId(message.getOperationIdentity());
        processingBusinessAction.setParams(message.getParams());
        processingBusinessAction.setState(NEED_TO_PROCESS);
        processingBusinessAction.setPersonalAccountId(message.getAccountId());

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

    public boolean existsActiveOperations(String personalAccountId, BusinessOperationType type, SimpleServiceMessage message) {
        Set<State> activeStates = new HashSet<>(Arrays.asList(NEED_TO_PROCESS, PROCESSING));

        Map<String, String> params = new HashMap<>();

        switch (type) {
            case SSL_CERTIFICATE_CREATE:
            case DATABASE_CREATE:
            case DATABASE_USER_CREATE:
            case FTP_USER_CREATE:
            case DOMAIN_CREATE:
            case WEB_SITE_CREATE:
                params.put("name", (String) message.getParam("name"));

                break;
            case RESOURCE_ARCHIVE_CREATE:
                params.put(RESOURCE_TYPE, (String) message.getParam(RESOURCE_TYPE));
                params.put(ARCHIVED_RESOURCE_ID_KEY, (String) message.getParam(ARCHIVED_RESOURCE_ID_KEY));

                break;
            case MAILBOX_CREATE:
                params.put("domainId", (String) message.getParam("domainId"));
                params.put("name", (String) message.getParam("name"));

                break;
            case APP_INSTALL:
                params.put("webSiteId", (String) message.getParam("webSiteId"));

                break;
            case REDIRECT_CREATE:
                params.put("domainId", (String) message.getParam("domainId"));

                break;
            case DNS_RECORD_CREATE:
                params.put("ownerName", (String) message.getParam("ownerName"));
                params.put("type", (String) message.getParam("type"));

                break;
            case DATABASE_DELETE:
            case DATABASE_USER_DELETE:
            case DNS_RECORD_DELETE:
            case DOMAIN_DELETE:
            case FTP_USER_DELETE:
            case MAILBOX_DELETE:
            case PERSON_DELETE:
            case REDIRECT_DELETE:
            case RESOURCE_ARCHIVE_DELETE:
            case SSL_CERTIFICATE_DELETE:
            case UNIX_ACCOUNT_DELETE:
            case WEB_SITE_DELETE:
            case DATABASE_UPDATE:
            case DATABASE_USER_UPDATE:
            case DNS_RECORD_UPDATE:
            case DOMAIN_UPDATE:
            case FTP_USER_UPDATE:
            case MAILBOX_UPDATE:
            case PERSON_UPDATE:
            case REDIRECT_UPDATE:
            case RESOURCE_ARCHIVE_UPDATE:
            case SSL_CERTIFICATE_UPDATE:
            case UNIX_ACCOUNT_UPDATE:
            case WEB_SITE_UPDATE:
                params.put("resourceId", (String) message.getParam("resourceId"));

                break;
            case PERSON_CREATE:
            case ACCOUNT_DELETE:
            case ACCOUNT_UPDATE:
            case FILE_BACKUP_RESTORE:
            case DATABASE_BACKUP_RESTORE:
            case ACCOUNT_CREATE:
            case SEO_ORDER:
            case UNIX_ACCOUNT_CREATE:
            case ACCOUNT_TRANSFER_REVERT:
            case COMMON_OPERATION:
            case ACCOUNT_TRANSFER:
            default:
                return false;
        }

        List<ProcessingBusinessOperation> operations = operationRepository.findAllByPersonalAccountIdAndTypeAndStateIn(personalAccountId, type, activeStates);

        return operations
                .stream()
                .anyMatch(o ->
                        params.entrySet()
                                .stream()
                                .allMatch(e ->
                                        e.getValue().equals((String) o.getParam(e.getKey()))
                                )
                );
    }
}
