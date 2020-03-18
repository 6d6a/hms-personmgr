package ru.majordomo.hms.personmgr.service;

import com.mongodb.client.result.UpdateResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.StringUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import ru.majordomo.hms.personmgr.common.BusinessActionType;
import ru.majordomo.hms.personmgr.common.BusinessOperationType;
import ru.majordomo.hms.personmgr.common.State;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.event.processingBusinessAction.ProcessingBusinessActionNewEvent;
import ru.majordomo.hms.personmgr.exception.InternalApiException;
import ru.majordomo.hms.personmgr.exception.ResourceNotFoundException;
import ru.majordomo.hms.personmgr.model.business.BusinessAction;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessAction;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessOperation;
import ru.majordomo.hms.personmgr.repository.BusinessActionRepository;
import ru.majordomo.hms.personmgr.repository.ProcessingBusinessActionRepository;
import ru.majordomo.hms.personmgr.repository.ProcessingBusinessOperationRepository;

import javax.annotation.Nullable;
import java.time.LocalDateTime;
import java.util.*;

import static ru.majordomo.hms.personmgr.common.BusinessOperationType.*;
import static ru.majordomo.hms.personmgr.common.Constants.*;
import static ru.majordomo.hms.personmgr.common.State.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class BusinessHelper {

    private final ProcessingBusinessOperationRepository operationRepository;
    private final BusinessActionRepository businessActionRepository;
    private final ProcessingBusinessActionRepository processingBusinessActionRepository;
    private final ApplicationEventPublisher publisher;
    private final MongoOperations mongoOperations;

    private final static Set<State> ACTIVE_STATES = Collections.unmodifiableSet(EnumSet.of(
            NEED_TO_PROCESS,
            PROCESSING
    ));

    @Nullable
    public <T extends Enum<T>> T getStage(String operationId, Class<T> enumType) {
        if (StringUtils.isEmpty(operationId)) {
            return null;
        }
        ProcessingBusinessOperation operation = operationRepository.findById(operationId).orElse(null);
        return getStage(operation, enumType);
    }

    @Nullable
    public <T extends Enum<T>> T getStage(ProcessingBusinessOperation operation, Class<T> enumType) {
        try {
            return Enum.valueOf(enumType, (String) operation.getParam("stage"));
        } catch (NullPointerException | ClassCastException | IllegalArgumentException ignore) {
            return null;
        }
    }

    public <T extends Enum<T>> boolean setStage(String operationId, Enum<T> stage) {
        UpdateResult updateResult = mongoOperations.updateFirst(
                Query.query(new Criteria("_id").is(operationId)),
                Update.update("updatedDate", LocalDateTime.now()).set("params.stage", stage),
                ProcessingBusinessOperation.class
        );
        boolean result = updateResult.getModifiedCount() == 1;
        log.debug("Tried setStage {} for operation: {} with result {}", stage, operationId, updateResult);
        return result;
    }

    @Nullable
    public ProcessingBusinessOperation findOperation(String operationId) {
        return operationRepository.findById(operationId).orElse(null);
    }

    /**
     * Атомарный переход на другую стадию. 
     * @param operationId - ProcessingBusinessOperation.id
     * @param stage - сталия на которую нужно изменить
     * @param needStage - стадия с которой возможен переход
     * @return - true в случае изменения. false - в случае если: не выпонены условия перехода, задание не существует, завершено с ошибокой и т.д.
     */
    public <T extends Enum<T>> boolean setStage(String operationId, Enum<T> stage, Enum<T> needStage) {
        UpdateResult updateResult = mongoOperations.updateFirst(
                Query.query(Criteria.where("_id").is(operationId).and("state").is(PROCESSING).and("params.stage").is(needStage)),
                Update.update("updatedDate", LocalDateTime.now()).set("params.stage", stage),
                ProcessingBusinessOperation.class
        );
        boolean result = updateResult.getModifiedCount() == 1;
        log.debug("Tried setStage {} for operation: {} with needStage {} and result {}", stage, operationId, needStage, updateResult);
        return result;
    }

    public void setErrorStatus(ProcessingBusinessOperation operation, String errorMessage) {
        operation.setState(ERROR);
        operation.addPublicParam("message", errorMessage);
        operation.setUpdatedDate(LocalDateTime.now());
        operationRepository.save(operation);
    }

    public void setErrorStatus(String operationId, String errorMessage) {
        operationRepository.findById(operationId).ifPresent(operation -> setErrorStatus(operation, errorMessage));
    }

    public boolean addWarning(String operationId, String warning) {
        if (StringUtils.isEmpty(operationId) || StringUtils.isEmpty(warning)) {
            return false;
        }

        UpdateResult result = mongoOperations.updateFirst(Query.query(new Criteria("_id").is(operationId)), Update.update("updatedDate", LocalDateTime.now()).push("publicParams.warnings", warning), ProcessingBusinessOperation.class);
        return result.getModifiedCount() == 1;
    }

    public boolean setParam(String operationId, String paramName, Object value) {
        if (StringUtils.isEmpty(operationId) || StringUtils.isEmpty(paramName)) {
            return false;
        }
        UpdateResult result = mongoOperations.updateFirst(Query.query(new Criteria("_id").is(operationId)), Update.update("updatedDate", LocalDateTime.now()).set("params." + paramName, value), ProcessingBusinessOperation.class);
        return result.getModifiedCount() == 1;
    }

    @Nullable
    public ProcessingBusinessOperation buildOperationAtomic(BusinessOperationType type, SimpleServiceMessage message) throws NotImplementedException {
        return buildOperationAtomic(type, message, null);
    }

    /**
     * Атомарное на уровне mongodb создание ProcessingBusinessOperation, позволит избежать выполнения одних и тех же заданий несколько раз,
     * даже если задания отправятся в разные экземпляры personmgr
     * @param type - тип операции
     * @param message - сообщение из которого создается операция.
     * @param publicParams - ничего или параметры которые будет видно снаружи, сразу при создании
     * @return - null если уже есть выполняемая операция или объект ProcessingBusinessOperation
     * @throws NotImplementedException - если указанный BusinessOperationType не поддерживается
     */
    @Nullable
    public ProcessingBusinessOperation buildOperationAtomic(BusinessOperationType type, SimpleServiceMessage message, @Nullable Map<String, Object> publicParams) throws NotImplementedException {
        Criteria criteria = new Criteria("state").in(ACTIVE_STATES)
                .and("personalAccountId").is(message.getAccountId());
        switch (type) {
            case DEDICATED_APP_SERVICE_CREATE:
            case DEDICATED_APP_SERVICE_UPDATE:
                String templateId = MapUtils.getString(message.getParams(), TEMPLATE_ID_KEY, "");
                if (templateId.isEmpty()) {
                    log.error("TemplateId required for atomic operation build for DedicatedAppService");
                    throw new InternalApiException();
                }
                criteria.and("params." + TEMPLATE_ID_KEY).is(templateId)
                        .and("type").in(DEDICATED_APP_SERVICE_CREATE, DEDICATED_APP_SERVICE_UPDATE);
                break;
            case IMPORT_FROM_BILLINGDB:
                criteria.and("type").is(IMPORT_FROM_BILLINGDB);
                break;
            default:
                throw new NotImplementedException("Atomic build ProcessingBusinessOperation not implemented for type: " + type);
        }
        Query query = Query.query(criteria);

        LocalDateTime now = LocalDateTime.now();
        Map<String, Object> copyPublicParams = MapUtils.isNotEmpty(publicParams) ? new HashMap<>(publicParams) : new HashMap<>();
        copyPublicParams.put("warnings", Collections.emptyList()); // создать массив publicParams.warnings заранее чтобы работал запрос $push

        Update update = new Update().setOnInsert("type", type).setOnInsert("createdDate", now).setOnInsert("updatedDate", now)
                .setOnInsert("params", message.getParams()).setOnInsert("publicParams", copyPublicParams)
                .setOnInsert("state", PROCESSING).setOnInsert("name", BUSINESS_OPERATION_TYPE2HUMAN.get(type))
                .setOnInsert("priority", 0).setOnInsert("_class", ProcessingBusinessOperation.class.getName());
        //если update создал новую запись, заполнить её полями для ProcessingBusinessOperation


        UpdateResult updateResult =  mongoOperations.upsert(query, update, ProcessingBusinessOperation.class);

        return updateResult.getUpsertedId() == null ? null : mongoOperations.findById(updateResult.getUpsertedId(), ProcessingBusinessOperation.class);
    }

    public ProcessingBusinessAction buildActionAndOperation(
            BusinessOperationType operationType,
            BusinessActionType actionType,
            SimpleServiceMessage message
    ) {
        ProcessingBusinessOperation processingBusinessOperation = buildOperation(operationType, message);

        return buildActionByOperation(actionType, message, processingBusinessOperation);
    }

    public ProcessingBusinessOperation buildOperation(BusinessOperationType operationType, SimpleServiceMessage message, @Nullable Map<String, Object> publicParam) {
        ProcessingBusinessOperation operation = new ProcessingBusinessOperation();

        operation.setPersonalAccountId(message.getAccountId());
        operation.setName(BUSINESS_OPERATION_TYPE2HUMAN.get(operationType));
        operation.setState(PROCESSING);
        operation.setParams(message.getParams());
        operation.setType(operationType);

        operation.addPublicParam("warnings", Collections.emptyList()); // создать массив publicParams.warnings заранее чтобы работал запрос $push
        String nameInParams = (String) message.getParam("name");
        if (nameInParams != null) {
            operation.addPublicParam("name", nameInParams);
        }
        if (MapUtils.isNotEmpty(publicParam)) {
            publicParam.forEach(operation::addPublicParam);
        }

        operationRepository.save(operation);

        return operation;
    }

    public ProcessingBusinessOperation buildOperation(BusinessOperationType operationType, SimpleServiceMessage message) {
        return buildOperation(operationType, message, null);
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

    public boolean existsActiveActions(String operationId) {
        return processingBusinessActionRepository.existsByOperationIdAndStateIn(operationId, ACTIVE_STATES);
    }

    public boolean existsActiveOperations(String personalAccountId, BusinessOperationType type, SimpleServiceMessage message) {

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
            case FILE_BACKUP_RESTORE:
            case DATABASE_BACKUP_RESTORE:
                params.put(DATASOURCE_URI_KEY, (String) message.getParam(DATASOURCE_URI_KEY));
                break;
            case IMPORT_FROM_BILLINGDB:
                break;
            case PERSON_CREATE:
            case ACCOUNT_DELETE:
            case ACCOUNT_UPDATE:
            case ACCOUNT_CREATE:
            case SEO_ORDER:
            case UNIX_ACCOUNT_CREATE:
            case ACCOUNT_TRANSFER_REVERT:
            case COMMON_OPERATION:
            case ACCOUNT_TRANSFER:
            default:
                return false;
        }

        LocalDateTime lowBorderOfTime = setLowBorderOfTime(type);

        List<ProcessingBusinessOperation> operations;
        if (lowBorderOfTime == null) {
            operations = operationRepository.findAllByPersonalAccountIdAndTypeAndStateIn(personalAccountId, type, ACTIVE_STATES);
        } else {
            operations = operationRepository.findAllByPersonalAccountIdAndTypeAndStateInAndCreatedDateGreaterThanEqual(personalAccountId, type, ACTIVE_STATES, lowBorderOfTime);
        }

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

    /**
     * В некоторых случаях в `processingBusinessOperation` остаются висеть задания со статусом "PROCESSING"
     * В следствие чего затруднительно определить выполняется операция или висит
     * @param type тип операции
     * @return временная метка, после которой выполнять поиск существующих операций, либо null
     */
    private LocalDateTime setLowBorderOfTime(BusinessOperationType type) {
        switch (type) {
            case RESOURCE_ARCHIVE_CREATE:
            case RESOURCE_ARCHIVE_DELETE:
                return LocalDateTime.now().minusHours(1L);
            default:
                return null;
        }
    }
}
