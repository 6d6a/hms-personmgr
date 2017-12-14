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

import ru.majordomo.hms.personmgr.common.BusinessOperationType;
import ru.majordomo.hms.personmgr.common.State;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.event.account.AccountCreatedEvent;
import ru.majordomo.hms.personmgr.event.accountHistory.AccountHistoryEvent;
import ru.majordomo.hms.personmgr.event.webSite.WebSiteCreatedEvent;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessAction;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessOperation;
import ru.majordomo.hms.personmgr.repository.ProcessingBusinessActionRepository;
import ru.majordomo.hms.personmgr.repository.ProcessingBusinessOperationRepository;
import ru.majordomo.hms.personmgr.service.AccountTransferService;
import ru.majordomo.hms.personmgr.service.AppsCatService;
import ru.majordomo.hms.personmgr.service.BusinessFlowDirector;

import static ru.majordomo.hms.personmgr.common.Constants.DATABASE_ID_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.DATABASE_USER_ID_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.DATABASE_USER_PASSWORD_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.HISTORY_MESSAGE_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.OPERATOR_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.PASSWORD_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.RESOURCE_ID_KEY;

public class CommonAmqpController {
    protected Logger logger = LoggerFactory.getLogger(getClass());

    BusinessFlowDirector businessFlowDirector;
    ProcessingBusinessActionRepository processingBusinessActionRepository;
    ProcessingBusinessOperationRepository processingBusinessOperationRepository;
    protected PersonalAccountManager accountManager;
    protected ApplicationEventPublisher publisher;
    private AppsCatService appsCatService;
    private AccountTransferService accountTransferService;

    protected String resourceName = "";

    protected String instanceName;

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
    public void setPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Autowired
    public void setAppsCatService(AppsCatService appsCatService) {
        this.appsCatService = appsCatService;
    }

    @Autowired
    public void setAccountTransferService(AccountTransferService accountTransferService) {
        this.accountTransferService = accountTransferService;
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

            saveAccountHistoryByMessageStateForDelete(message, state);
            saveLogByMessageStateForDelete(message, state);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Got Exception in handleDeleteEventFromRc " + e.getMessage());
        }
    }

    private void saveAccountHistoryByMessageState(SimpleServiceMessage message, State state, String action) {
        if (state.equals(State.PROCESSED)) {
            ProcessingBusinessAction businessAction = processingBusinessActionRepository.findOne(message.getActionIdentity());

            if (businessAction != null) {
                //Save history
                Map<String, String> params = new HashMap<>();
                params.put(HISTORY_MESSAGE_KEY, "Заявка на " + action + " ресурса '" +
                        resourceName + "' выполнена успешно (имя: " + message.getParam("name") + ")");
                params.put(OPERATOR_KEY, "service");

                publisher.publishEvent(new AccountHistoryEvent(businessAction.getPersonalAccountId(), params));
            }
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
            ProcessingBusinessAction businessAction = processingBusinessActionRepository.findOne(message.getActionIdentity());

            if (businessAction != null) {
                PersonalAccount account = accountManager.findOne(message.getAccountId());

                Map<String, String> params = new HashMap<>();

                ProcessingBusinessOperation businessOperation;

                switch (businessAction.getBusinessActionType()) {
                    case WEB_SITE_CREATE_RC:

                        SimpleServiceMessage mailMessage = new SimpleServiceMessage();
                        mailMessage.setAccountId(account.getId());

                        String resourceId = getResourceIdByObjRef(message.getObjRef());

                        params.put(RESOURCE_ID_KEY, resourceId);

                        publisher.publishEvent(new WebSiteCreatedEvent(account, params));

                        break;

                    case UNIX_ACCOUNT_CREATE_RC:
                        businessOperation = processingBusinessOperationRepository.findOne(message.getOperationIdentity());
                        if (businessOperation != null && businessOperation.getType() == BusinessOperationType.ACCOUNT_CREATE) {
                            businessOperation.setState(State.PROCESSED);
                            processingBusinessOperationRepository.save(businessOperation);

                            params.put(PASSWORD_KEY, (String) businessOperation.getParam(PASSWORD_KEY));

                            publisher.publishEvent(new AccountCreatedEvent(account, params));
                        }
                        break;

                    case DATABASE_USER_CREATE_RC:
                        businessOperation = processingBusinessOperationRepository.findOne(message.getOperationIdentity());
                        if (businessOperation != null && businessOperation.getType() == BusinessOperationType.APP_INSTALL) {
                            String databaseUserId = getResourceIdByObjRef(message.getObjRef());

                            businessOperation.addParam(DATABASE_USER_ID_KEY, databaseUserId);
                            businessOperation.addParam(DATABASE_USER_PASSWORD_KEY, businessAction.getParam(DATABASE_USER_PASSWORD_KEY));

                            processingBusinessOperationRepository.save(businessOperation);

                            message.setParams(businessOperation.getParams());
                            appsCatService.addDatabase(message);
                        }
                        break;

                    case DATABASE_CREATE_RC:
                        businessOperation = processingBusinessOperationRepository.findOne(message.getOperationIdentity());
                        if (businessOperation != null && businessOperation.getType() == BusinessOperationType.APP_INSTALL) {
                            String databaseId = getResourceIdByObjRef(message.getObjRef());

                            businessOperation.addParam(DATABASE_ID_KEY, databaseId);
                            businessOperation.addParam("DB_NAME", businessAction.getParam("DB_NAME"));

                            processingBusinessOperationRepository.save(businessOperation);

                            message.setParams(businessOperation.getParams());
                            appsCatService.processInstall(message);
                        }
                        break;
                }
            }
        }
    }

    private void processEventsByMessageStateForUpdate(SimpleServiceMessage message, State state) {
        ProcessingBusinessAction businessAction = processingBusinessActionRepository.findOne(message.getActionIdentity());

        if (businessAction != null) {
            ProcessingBusinessOperation businessOperation;

            switch (businessAction.getBusinessActionType()) {
                case UNIX_ACCOUNT_UPDATE_RC:
                case DATABASE_USER_UPDATE_RC:
                case DATABASE_UPDATE_RC:
                    businessOperation = processingBusinessOperationRepository.findOne(message.getOperationIdentity());
                    if (businessOperation != null && businessOperation.getType() == BusinessOperationType.ACCOUNT_TRANSFER) {
                        if (state.equals(State.PROCESSED)) {
                            accountTransferService.checkOperationAfterUnixAccountAndDatabaseUpdate(businessOperation);
                        } else if (state.equals(State.ERROR)) {
                            businessOperation.setState(State.ERROR);
                            processingBusinessOperationRepository.save(businessOperation);

                            accountTransferService.revertTransferUnixAccountAndDatabase(businessOperation);
                        }
                    }
                    break;
                case WEB_SITE_UPDATE_RC:
                    businessOperation = processingBusinessOperationRepository.findOne(message.getOperationIdentity());
                    if (businessOperation != null && businessOperation.getType() == BusinessOperationType.ACCOUNT_TRANSFER) {
                        if (state.equals(State.PROCESSED)) {
                            accountTransferService.checkOperationAfterWebSiteUpdate(businessOperation);
                        } else if (state.equals(State.ERROR)) {
                            businessOperation.setState(State.ERROR);
                            processingBusinessOperationRepository.save(businessOperation);

                            accountTransferService.revertTransferUnixAccountAndDatabase(businessOperation);
                            accountTransferService.revertTransferWebSites(businessOperation);
                        }
                    }
                    break;
                case DNS_RECORD_UPDATE_RC:
                    businessOperation = processingBusinessOperationRepository.findOne(message.getOperationIdentity());
                    if (businessOperation != null && businessOperation.getType() == BusinessOperationType.ACCOUNT_TRANSFER) {
                        if (state.equals(State.PROCESSED)) {
                            accountTransferService.finishOperation(businessOperation);
                        }
                    }
                    break;
            }
        }
    }
}
