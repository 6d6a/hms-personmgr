package ru.majordomo.hms.personmgr.controller.amqp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import ru.majordomo.hms.personmgr.common.BusinessActionType;
import ru.majordomo.hms.personmgr.common.BusinessOperationType;
import ru.majordomo.hms.personmgr.common.State;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.event.account.AccountCreatedEvent;
import ru.majordomo.hms.personmgr.manager.AccountHistoryManager;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessAction;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessOperation;
import ru.majordomo.hms.personmgr.repository.ProcessingBusinessActionRepository;
import ru.majordomo.hms.personmgr.repository.ProcessingBusinessOperationRepository;
import ru.majordomo.hms.personmgr.service.AccountTransferService;
import ru.majordomo.hms.personmgr.service.AmqpSender;
import ru.majordomo.hms.personmgr.service.BusinessFlowDirector;
import ru.majordomo.hms.personmgr.service.BusinessHelper;
import ru.majordomo.hms.personmgr.service.FtpUserService;
import ru.majordomo.hms.personmgr.service.ResourceArchiveService;
import ru.majordomo.hms.personmgr.service.ResourceChecker;
import ru.majordomo.hms.rc.user.resources.ResourceArchiveType;

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
            State state = businessFlowDirector.processMessage(message);

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

    void handleUpdateEventFromRc(SimpleServiceMessage message, Map<String, String> headers) {
        String provider = headers.get("provider");
        logger.debug("Received update message from " + provider + ": " + message.toString());

        try {
            State state = businessFlowDirector.processMessage(message);

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
            State state = businessFlowDirector.processMessage(message);

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
                                    }
                                });
                        break;

                    case DATABASE_USER_CREATE_RC:
                        processingBusinessOperationRepository.findById(message.getOperationIdentity())
                                .ifPresent(operation -> sendToAppscat(message, operation, DATABASE_USER_CREATE));

                        break;

                    case DATABASE_CREATE_RC:
                        processingBusinessOperationRepository.findById(message.getOperationIdentity())
                                .ifPresent(operation -> sendToAppscat(message, operation, DATABASE_CREATE));

                        break;

                    case FTP_USER_CREATE_RC:
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

    private void processEventsByMessageStateForUpdate(SimpleServiceMessage message, State state) {
        if (message.getOperationIdentity() == null) { return; }

        processingBusinessActionRepository.findById(message.getActionIdentity())
                .ifPresent(action -> {

            switch (action.getBusinessActionType()) {
                case UNIX_ACCOUNT_UPDATE_RC:
                case DATABASE_USER_UPDATE_RC:
                case DATABASE_UPDATE_RC:
                    processingBusinessOperationRepository.findById(message.getOperationIdentity())
                            .ifPresent(operation -> {
                                if (operation.getType() == BusinessOperationType.ACCOUNT_TRANSFER) {
                                    if (state.equals(State.PROCESSED)) {
                                        accountTransferService.checkOperationAfterUnixAccountAndDatabaseUpdate(operation);
                                    } else if (state.equals(State.ERROR)) {
                                        operation.setState(State.ERROR);
                                        processingBusinessOperationRepository.save(operation);

                                        accountTransferService.revertTransfer(operation);
                                    }
                                } else if (operation.getType() == BusinessOperationType.APP_INSTALL
                                        && action.getBusinessActionType() == BusinessActionType.DATABASE_UPDATE_RC) {
                                    try {
                                        amqpSender.send(DATABASE_UPDATE, APPSCAT_ROUTING_KEY, message);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            });

                    break;
                case WEB_SITE_UPDATE_RC:
                    processingBusinessOperationRepository.findById(message.getOperationIdentity())
                            .ifPresent(operation -> {
                                if (operation.getType() == BusinessOperationType.ACCOUNT_TRANSFER) {
                                    if (state.equals(State.PROCESSED)) {
                                        accountTransferService.checkOperationAfterWebSiteUpdate(operation);
                                    } else if (state.equals(State.ERROR)) {
                                        operation.setState(State.ERROR);
                                        processingBusinessOperationRepository.save(operation);

                                        accountTransferService.revertTransferOnWebSitesFail(operation);
                                    }
                                } else if (operation.getType() == BusinessOperationType.APP_INSTALL) {
                                    try {
                                        amqpSender.send(WEBSITE_UPDATE, APPSCAT_ROUTING_KEY, message);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
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
            }
            });
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
