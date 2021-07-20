package ru.majordomo.hms.personmgr.controller.amqp;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import ru.majordomo.hms.personmgr.common.*;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.event.account.AccountCreatedEvent;
import ru.majordomo.hms.personmgr.importing.DBImportService;
import ru.majordomo.hms.personmgr.manager.AccountHistoryManager;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessAction;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessOperation;
import ru.majordomo.hms.personmgr.repository.ProcessingBusinessActionRepository;
import ru.majordomo.hms.personmgr.repository.ProcessingBusinessOperationRepository;
import ru.majordomo.hms.personmgr.service.*;
import ru.majordomo.hms.rc.user.resources.ResourceArchiveType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static ru.majordomo.hms.personmgr.common.Constants.APPSCAT_ROUTING_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.Exchanges.DATABASE_CREATE;
import static ru.majordomo.hms.personmgr.common.Constants.Exchanges.DATABASE_UPDATE;
import static ru.majordomo.hms.personmgr.common.Constants.Exchanges.DATABASE_USER_CREATE;
import static ru.majordomo.hms.personmgr.common.Constants.Exchanges.WEBSITE_UPDATE;
import static ru.majordomo.hms.personmgr.common.Constants.LONG_LIFE;
import static ru.majordomo.hms.personmgr.common.Constants.PASSWORD_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.RESOURCE_ID_KEY;

public class CommonAmqpController {
    protected Logger logger = LoggerFactory.getLogger(getClass());

    BusinessFlowDirector businessFlowDirector;
    ProcessingBusinessActionRepository processingBusinessActionRepository;
    ProcessingBusinessOperationRepository processingBusinessOperationRepository;
    protected PersonalAccountManager accountManager;
    protected ApplicationEventPublisher publisher;
    protected BusinessHelper businessHelper;
    protected ResourceChecker resourceChecker;
    private AccountTransferService accountTransferService;
    protected AmqpSender amqpSender;
    private FtpUserService ftpUserService;
    protected AccountHistoryManager history;
    private ResourceArchiveService resourceArchiveService;

    @Setter
    @Autowired
    private DedicatedAppServiceHelper dedicatedAppServiceHelper;

    @Setter
    @Autowired
    private ResourceHelper resourceHelper;

    @Setter
    @Nullable
    @Autowired(required = false)
    private DBImportService dbImportService;

    @Getter
    protected String resourceName = "";

    protected String instanceName;


    @Autowired
    public void setAccountHistoryService(AccountHistoryManager history) {
        this.history = history;
    }

    @Autowired
    public void setBusinessFlowDirector(BusinessFlowDirector businessFlowDirector) {
        this.businessFlowDirector = businessFlowDirector;
    }

    @Autowired
    public void setProcessingBusinessActionRepository(
            ProcessingBusinessActionRepository processingBusinessActionRepository
    ) {
        this.processingBusinessActionRepository = processingBusinessActionRepository;
    }

    @Autowired
    public void setProcessingBusinessOperationRepository(
            ProcessingBusinessOperationRepository processingBusinessOperationRepository
    ) {
        this.processingBusinessOperationRepository = processingBusinessOperationRepository;
    }

    @Autowired
    public void setAccountManager(PersonalAccountManager accountManager) {
        this.accountManager = accountManager;
    }

    @Autowired
    public void setBusinessHelper(BusinessHelper businessHelper) {
        this.businessHelper = businessHelper;
    }

    @Autowired
    public void setResourceChecker(ResourceChecker resourceChecker) {
        this.resourceChecker = resourceChecker;
    }

    @Autowired
    public void setPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Autowired
    public void setAccountTransferService(AccountTransferService accountTransferService) {
        this.accountTransferService = accountTransferService;
    }

    @Autowired
    public void setAmqpSender(AmqpSender amqpSender) {
        this.amqpSender = amqpSender;
    }

    @Autowired
    public void setFtpUserService(FtpUserService ftpUserService) {
        this.ftpUserService = ftpUserService;
    }

    @Autowired
    public void setResourceArchiveService(ResourceArchiveService resourceArchiveService) {
        this.resourceArchiveService = resourceArchiveService;
    }

    @Value("${hms.instance.name}")
    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    @Nullable
    private String getResourceIdByObjRef(String url) {
        try {
            URL processingUrl = new URL(url);
            String path = processingUrl.getPath();
            String[] pathParts = path.split("/");

            return pathParts[2];
        } catch (MalformedURLException e) {
            e.printStackTrace();
            logger.error("Got Exception in ru.majordomo.hms.personmgr.controller.amqp.CommonAmqpController.getResourceIdByObjRef " + e.getMessage());
            return null;
        }
    }

    void handleCreateEventFromRc(SimpleServiceMessage message, Map<String, String> headers) {
        String provider = headers.get("provider");
        logger.debug("Received create message from " + provider + ": " + message.toString());

        try {
            State state = businessFlowDirector.processMessage(message, resourceName);

            processEventsByMessageStateForCreate(message, state);
            saveAccountHistoryByMessageStateForCreate(message, state);
            saveLogByMessageStateForCreate(message, state);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Got Exception in handleCreateEventFromRc " + e.getMessage());
        }
    }

    void handleUpdateEventFromTE(SimpleServiceMessage message, Map<String, String> headers) {
        handleUpdateEventFromRc(message, headers);
    }

    /**
     * Обработка сообщений об обновлении для всех ресурсов
     * @param message payload сообщения из rabbit от rc-user.
     *                основное в message.getParams(): {@code {success: boolean, errorMessage?: string, exceptionClass?: string} }
     *
     * Например ошибка на стороне rc-user для домена:
     * <code>
     * {
     *   "operationIdentity": "60e83771f0dffd1711c90b0a",
     *   "actionIdentity": "60e837d1f0dffd1711c90b1a",
     *   "accountId": "189986",
     *   "objRef": null,
     *   "params": {
     *     "success": false,
     *     "errorMessage": "Обработка ресурса не удалась: Ресурс в процессе обновления",
     *     "originalParams": {},
     *     "exceptionClass": "ParameterValidationException",
     *     "errors": [
     *       {
     *         "property": "domain",
     *         "errors": [
     *           "Ресурс в процессе обновления"
     *         ]
     *       }
     *     ]
     *   }
     * }
     * </code>
     *
     * Например ошибка на стороне te для пользователя баз данных:
     * <code>
     * {
     *   "operationIdentity": "60e83a9cf0dffd1711c90c1f",
     *   "actionIdentity": "60e83a9ff0dffd1711c90c59",
     *   "accountId": "189986",
     *   "objRef": "http://rc-user/database-user/5ef0d1dbf5ffb3000169bb0e",
     *   "params": {
     *     "ovsId": "60e83aa6f0dffd150a55817b",
     *     "success": false,
     *     "name": "u189986_raot",
     *     "errorMessage": "We got randomly generated error from teremock",
     *     "originalParams": {},
     *     "ovs": {
     *       "id": "60e83aa6f0dffd150a55817b",
     *       "resourceId": "5ef0d1dbf5ffb3000169bb0e",
     *       "resource": {},
     *       "affectedResources": [],
     *       "requiredResources": [],
     *       "replace": false
     *     }
     *   }
     * }
     * </code>
     * @param headers заголовки rabbit
     */
    void handleUpdateEventFromRc(SimpleServiceMessage message, Map<String, String> headers) {
        String provider = headers.get("provider");
        logger.debug("Received update message from " + provider + ": " + message.toString());

        try {
            State state = businessFlowDirector.processMessage(message, resourceName);

            processEventsByMessageStateForUpdate(message, state);
            saveAccountHistoryByMessageStateForUpdate(message, state);
            saveLogByMessageStateForUpdate(message, state);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Got Exception in handleUpdateEventFromRc " + e.getMessage());
        }
    }

    void handleDeleteEventFromRc(SimpleServiceMessage message, Map<String, String> headers) {
        String provider = headers.get("provider");
        logger.debug("Received delete message from " + provider + ": " + message.toString());

        try {
            State state = businessFlowDirector.processMessage(message, resourceName);

            processEventsByMessageStateForDelete(message, state);
            saveAccountHistoryByMessageStateForDelete(message, state);
            saveLogByMessageStateForDelete(message, state);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Got Exception in handleDeleteEventFromRc " + e.getMessage());
        }
    }

    private void saveAccountHistoryByMessageState(SimpleServiceMessage message, State state, String action) {
        if (state.equals(State.PROCESSED)) {
            processingBusinessActionRepository.findById(message.getActionIdentity()).ifPresent(businessAction -> {
                String historyMessage = "Заявка на " + action + " ресурса '" +
                        resourceName + "' выполнена успешно (имя: " + message.getParam("name") + ")";

                history.save(businessAction.getPersonalAccountId(), historyMessage, "service");
            });
        }
    }

    private void saveAccountHistoryByMessageStateForCreate(SimpleServiceMessage message, State state) {
        saveAccountHistoryByMessageState(message, state, "создание");
    }

    private void saveAccountHistoryByMessageStateForUpdate(SimpleServiceMessage message, State state) {
        saveAccountHistoryByMessageState(message, state, "обновление");
    }

    private void saveAccountHistoryByMessageStateForDelete(SimpleServiceMessage message, State state) {
        saveAccountHistoryByMessageState(message, state, "удаление");
    }


    private void saveLogByMessageState(SimpleServiceMessage message, State state, String action) {
        String logMessage = "ACTION_IDENTITY: " + message.getActionIdentity() +
                " OPERATION_IDENTITY: " + message.getOperationIdentity() +
                " " + action + " ресурса " + resourceName + " " + message.getAccountId();

        switch (state) {
            case PROCESSED:
                logger.info(logMessage + " завершено успешно");
                break;
            case ERROR:
                logger.error(logMessage + " не удалось");
                break;
        }
    }

    void saveLogByMessageStateForCreate(SimpleServiceMessage message, State state) {
        saveLogByMessageState(message, state, "Создание");
    }

    private void saveLogByMessageStateForUpdate(SimpleServiceMessage message, State state) {
        saveLogByMessageState(message, state, "Обновление");
    }

    private void saveLogByMessageStateForDelete(SimpleServiceMessage message, State state) {
        saveLogByMessageState(message, state, "Удаление");
    }

    private void processEventsByMessageStateForCreate(SimpleServiceMessage message, State state) {
        if (state.equals(State.PROCESSED)) {
            processingBusinessActionRepository.findById(message.getActionIdentity()).ifPresent(businessAction -> {

                PersonalAccount account = accountManager.findOne(businessAction.getPersonalAccountId());

                Map<String, String> params = new HashMap<>();

                switch (businessAction.getBusinessActionType()) {
                    case UNIX_ACCOUNT_CREATE_RC:
                        processingBusinessOperationRepository.findById(message.getOperationIdentity())
                                .ifPresent(operation -> {
                                    if (operation.getType() == BusinessOperationType.ACCOUNT_CREATE) {
                                        operation.setState(State.PROCESSED);
                                        processingBusinessOperationRepository.save(operation);

                                        params.put(PASSWORD_KEY, (String) operation.getParam(PASSWORD_KEY));

                                        publisher.publishEvent(new AccountCreatedEvent(account, params));
                                    } else if (operation.getType() == BusinessOperationType.IMPORT_FROM_BILLINGDB && dbImportService != null) {
                                        dbImportService.startStageSecondIfNeed(operation);
                                    }
                                });
                        break;

                    case DATABASE_USER_CREATE_RC:
                        processingBusinessOperationRepository.findById(message.getOperationIdentity())
                                .ifPresent(operation -> {
                                    if (operation.getType() == BusinessOperationType.IMPORT_FROM_BILLINGDB && dbImportService != null) {
                                        dbImportService.startStageSecondIfNeed(operation);
                                    } else {
                                        sendToAppscat(message, operation, DATABASE_USER_CREATE);
                                    }
                                });

                        break;

                    case DATABASE_CREATE_RC:
                        processingBusinessOperationRepository.findById(message.getOperationIdentity())
                                .ifPresent(operation -> {
                                    if (dbImportService != null && operation.getType() == BusinessOperationType.IMPORT_FROM_BILLINGDB) {
                                        dbImportService.finishImportIfNeed(operation);
                                    } else {
                                        sendToAppscat(message, operation, DATABASE_CREATE);
                                    }
                                });

                        break;

                    case FTP_USER_CREATE_RC:
                        processingBusinessOperationRepository.findById(message.getOperationIdentity()).ifPresent(operation -> {
                            if (dbImportService != null && operation.getType() == BusinessOperationType.IMPORT_FROM_BILLINGDB) {
                                dbImportService.finishImportIfNeed(operation);
                            }
                        });
                        ftpUserService.processServices(account);
                        break;

                    case RESOURCE_ARCHIVE_CREATE_RC:
                        processingBusinessOperationRepository.findById(message.getOperationIdentity())
                                .ifPresent(operation -> {

                                    String resourceId = getResourceIdByObjRef(message.getObjRef());

                                    operation.addParam(RESOURCE_ID_KEY, resourceId);

                                    processingBusinessOperationRepository.save(operation);

                                    if (operation.getParam(LONG_LIFE) != null && (boolean) operation.getParam(LONG_LIFE)) {
                                        resourceArchiveService.createFromProcessingBusinessOperation(operation);
                                    }

                                    resourceArchiveService.notifyByProcessingBusinessOperation(operation);
                                });

                        break;

                    case DEDICATED_APP_SERVICE_CREATE_RC_STAFF:
                        processingBusinessOperationRepository.findById(message.getOperationIdentity()).ifPresent(operation -> {
                            String resourceId = getResourceIdByObjRef(message.getObjRef());
                            dedicatedAppServiceHelper.processAmqpEventCreateRcStaffFinish(operation, resourceId);
                        });
                        break;
                    case WEB_SITE_CREATE_RC:
                        processingBusinessOperationRepository.findById(message.getOperationIdentity()).ifPresent(operation -> {
                            if (dbImportService != null && operation.getType() == BusinessOperationType.IMPORT_FROM_BILLINGDB) {
                                dbImportService.finishImportIfNeed(operation);
                            }
                        });
                        break;
                    case DOMAIN_CREATE_RC:
                        ProcessingBusinessOperation operation = message.getOperationIdentity() == null ? null :
                                processingBusinessOperationRepository.findById(message.getOperationIdentity()).orElse(null);
                        if (operation == null) {
                            break;
                        }
                        if (operation.getType() == BusinessOperationType.DOMAIN_CREATE_CHANGE_WEBSITE) {
                            String domainId = getResourceIdByObjRef(message.getObjRef());
                            resourceHelper.processEventsAmqpCreateDomainAndChangeWebsite(message, operation, domainId);
                        }
                        break;
                }
            });
        }
    }

    private void sendToAppscat(SimpleServiceMessage message, ProcessingBusinessOperation businessOperation, String exchange) {
        if (businessOperation != null) {
            if (businessOperation.getType() == BusinessOperationType.APP_INSTALL) {
                try {
                    amqpSender.send(exchange, APPSCAT_ROUTING_KEY, message);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void processEventsByMessageStateForUpdate(@Nonnull SimpleServiceMessage message, @Nonnull State state) {
        if (message.getOperationIdentity() == null) { return; }

        ProcessingBusinessAction action = message.getActionIdentity() == null ? null :
                processingBusinessActionRepository.findById(message.getActionIdentity()).orElse(null);
        if (action == null) { return; }

        switch (action.getBusinessActionType()) {
            case UNIX_ACCOUNT_UPDATE_RC:
                processingBusinessOperationRepository.findById(message.getOperationIdentity()).ifPresent(operation -> {
                    switch (operation.getType()) {
                        case ACCOUNT_TRANSFER:
                            accountTransferService.processEventsAmqpForUnixAccountAndDatabaseUpdate(operation, state);
                            break;
                        case SWITCH_ACCOUNT_RESOURCES:
                            resourceHelper.processEventsAmqpSwitchStartStageSecondIfNeed(operation);
                            break;
                    }
                });
                break;
            case DATABASE_UPDATE_RC:
                processingBusinessOperationRepository.findById(message.getOperationIdentity()).ifPresent(operation -> {
                    switch (operation.getType()) {
                        case ACCOUNT_TRANSFER:
                            accountTransferService.processEventsAmqpForUnixAccountAndDatabaseUpdate(operation, state);
                            break;
                        case SWITCH_ACCOUNT_RESOURCES:
                            resourceHelper.processEventsAmqpSwitchResourcesFinishIfNeed(operation); // Not used now!
                            break;
                        case APP_INSTALL:
                            try {
                                amqpSender.send(DATABASE_UPDATE, APPSCAT_ROUTING_KEY, message);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                    }
                });
                break;
            case DATABASE_USER_UPDATE_RC:
                processingBusinessOperationRepository.findById(message.getOperationIdentity()).ifPresent(operation -> {
                    switch (operation.getType()) {
                        case ACCOUNT_TRANSFER:
                            accountTransferService.processEventsAmqpForUnixAccountAndDatabaseUpdate(operation, state);
                            break;
                        case SWITCH_ACCOUNT_RESOURCES:
                            resourceHelper.processEventsAmqpSwitchResourcesFinishIfNeed(operation);
                            break;
                    }
                });
                break;
            case WEB_SITE_UPDATE_RC:
                processingBusinessOperationRepository.findById(message.getOperationIdentity()).ifPresent(operation -> {
                    switch (operation.getType()) {
                        case ACCOUNT_TRANSFER:
                            if (state.equals(State.PROCESSED)) {
                                accountTransferService.checkOperationAfterWebSiteUpdate(operation);
                            } else if (state.equals(State.ERROR)) {
                                operation.setState(State.ERROR);
                                processingBusinessOperationRepository.save(operation);

                                accountTransferService.revertTransferOnWebSitesFail(operation);
                            }
                            break;
                        case APP_INSTALL:
                            try {
                                amqpSender.send(WEBSITE_UPDATE, APPSCAT_ROUTING_KEY, message);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            break;
                        case WEB_SITE_UPDATE_EXTENDED_ACTION:
                            if (State.PROCESSED.equals(state)) {
                                ExtendedActionStage stage = businessHelper.getStage(operation, ExtendedActionStage.class);
                                if (EnumSet.of(ExtendedActionStage.BEFORE_FULL_SHELLUPDATE, ExtendedActionStage.BEFORE_FULL_SHELL).contains(stage)) {
                                    SimpleServiceMessage actionMessage = new SimpleServiceMessage(operation.getPersonalAccountId(), operation.getId(), null);
                                    String newAction;
                                    if (stage == ExtendedActionStage.BEFORE_FULL_SHELL) {
                                        stage = ExtendedActionStage.FULL_SHELL;
                                        newAction = ExtendedActionConstants.SHELL;
                                    } else {
                                        stage = ExtendedActionStage.FULL_SHELLUPDATE;
                                        newAction = ExtendedActionConstants.SHELLUPDATE;
                                    }
                                    businessHelper.setStage(operation.getId(), stage);
                                    actionMessage.addParam(ExtendedActionConstants.EXTENDED_ACTION_KEY, newAction);
                                    actionMessage.addParam(RESOURCE_ID_KEY, operation.getParam(RESOURCE_ID_KEY));
                                    businessHelper.buildActionByOperation(BusinessActionType.WEB_SITE_UPDATE_RC, actionMessage, operation);
                                } else {
                                    operation.setState(State.PROCESSED);
                                    processingBusinessOperationRepository.save(operation);
                                }
                            } else if (State.ERROR.equals(state)){
                                operation.setState(State.ERROR);
                                processingBusinessOperationRepository.save(operation);
                            }
                            break;
                        case SWITCH_ACCOUNT_RESOURCES:
                            resourceHelper.processEventsAmqpSwitchResourcesFinishIfNeed(operation);
                            break;

                    }
                });
                break;
            case DNS_RECORD_UPDATE_RC:
                processingBusinessOperationRepository.findById(message.getOperationIdentity())
                        .ifPresent(operation -> {
                            if (operation.getType() == BusinessOperationType.ACCOUNT_TRANSFER) {
                                if (state.equals(State.PROCESSED)) {
                                    accountTransferService.finishOperation(operation);
                                }
                            }
                        });

                break;
            case DEDICATED_APP_SERVICE_UPDATE_RC_STAFF:
                String staffServiceId = getResourceIdByObjRef(message.getObjRef());
                dedicatedAppServiceHelper.processAmqpEventUpdateRcStaffFinish(action, staffServiceId);
                processingBusinessOperationRepository.findById(message.getOperationIdentity()).ifPresent(operation -> {
                    if (operation.getType() == BusinessOperationType.SWITCH_ACCOUNT_RESOURCES) {
                        resourceHelper.processEventsAmqpSwitchResourcesFinishIfNeed(operation);
                    }
                });
                break;
            case DOMAIN_UPDATE_RC:
                processingBusinessOperationRepository.findById(message.getOperationIdentity()).ifPresent(operation -> {
                    if (operation.getType() == BusinessOperationType.SWITCH_ACCOUNT_RESOURCES) {
                        resourceHelper.processEventsAmqpSwitchStartStageSecondIfNeed(operation);
                    }
                });
                break;
            case MAILBOX_UPDATE_RC:
            case REDIRECT_UPDATE_RC:
            case FTP_USER_UPDATE_RC:
                processingBusinessOperationRepository.findById(message.getOperationIdentity()).ifPresent(operation -> {
                    if (operation.getType() == BusinessOperationType.SWITCH_ACCOUNT_RESOURCES) {
                        resourceHelper.processEventsAmqpSwitchResourcesFinishIfNeed(operation);
                    }
                });
                break;
        }
    }

    private void processEventsByMessageStateForDelete(SimpleServiceMessage message, State state) {
        processingBusinessActionRepository.findById(message.getActionIdentity()).ifPresent(action -> {

            PersonalAccount account = accountManager.findOne(action.getPersonalAccountId());

            switch (action.getBusinessActionType()) {
                case FTP_USER_DELETE_RC:
                    ftpUserService.processServices(account);

                    break;
                case RESOURCE_ARCHIVE_DELETE_RC:
                    if (state.equals(State.PROCESSED)) {
                        processingBusinessOperationRepository.findById(message.getOperationIdentity())
                                .ifPresent(operation -> {
                                    if (operation.getParam(LONG_LIFE) != null && (boolean) operation.getParam(LONG_LIFE)) {
                                        resourceArchiveService.deleteLongLifeResourceArchiveAndAccountService(operation);
                                    }
                                });
                    }

                    break;
                case WEB_SITE_DELETE_RC:
                    if (state.equals(State.PROCESSED)) {
                        processingBusinessOperationRepository.findById(message.getOperationIdentity())
                                .ifPresent(operation -> {
                                    String resourceId = getResourceIdByObjRef(message.getObjRef());

                                    resourceArchiveService.deleteLongLifeResourceArchiveAndAccountService(
                                            operation.getPersonalAccountId(),
                                            ResourceArchiveType.WEBSITE,
                                            resourceId
                                    );
                                    if (operation.getType() == BusinessOperationType.IMPORT_FROM_BILLINGDB && dbImportService != null) {
                                        dbImportService.startStageFirstIfNeed(operation);
                                    }
                        });
                    }

                    break;
                case DATABASE_DELETE_RC:
                    if (state.equals(State.PROCESSED)) {
                        processingBusinessOperationRepository.findById(message.getOperationIdentity())
                                .ifPresent(operation -> {
                                    String resourceId = getResourceIdByObjRef(message.getObjRef());

                                    resourceArchiveService.deleteLongLifeResourceArchiveAndAccountService(
                                            operation.getPersonalAccountId(),
                                            ResourceArchiveType.DATABASE,
                                            resourceId
                                    );
                        });
                    }

                    break;
            }
        });
    }
}
