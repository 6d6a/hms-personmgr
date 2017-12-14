package ru.majordomo.hms.personmgr.service;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ru.majordomo.hms.personmgr.common.BusinessActionType;
import ru.majordomo.hms.personmgr.common.BusinessOperationType;
import ru.majordomo.hms.personmgr.common.State;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.dto.AccountTransferRequest;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessAction;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessOperation;
import ru.majordomo.hms.personmgr.repository.ProcessingBusinessActionRepository;
import ru.majordomo.hms.personmgr.repository.ProcessingBusinessOperationRepository;
import ru.majordomo.hms.rc.staff.resources.Server;
import ru.majordomo.hms.rc.staff.resources.Service;
import ru.majordomo.hms.rc.user.resources.DNSResourceRecord;
import ru.majordomo.hms.rc.user.resources.DNSResourceRecordType;
import ru.majordomo.hms.rc.user.resources.Database;
import ru.majordomo.hms.rc.user.resources.DatabaseUser;
import ru.majordomo.hms.rc.user.resources.Domain;
import ru.majordomo.hms.rc.user.resources.UnixAccount;
import ru.majordomo.hms.rc.user.resources.WebSite;

import static ru.majordomo.hms.personmgr.common.Constants.DATASOURCE_URI_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.DATA_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.DATA_POSTPROCESSOR_ARGS_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.DATA_POSTPROCESSOR_STRING_REPLACE_ACTION;
import static ru.majordomo.hms.personmgr.common.Constants.DATA_POSTPROCESSOR_STRING_REPLACE_STRING_ARG;
import static ru.majordomo.hms.personmgr.common.Constants.DATA_POSTPROCESSOR_STRING_SEARCH_PATTERN_ARG;
import static ru.majordomo.hms.personmgr.common.Constants.DATA_POSTPROCESSOR_TYPE_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.DNS_RECORD_SENT_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.NEW_DATABASE_HOST_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.NEW_DATABASE_SERVER_ID_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.NEW_UNIX_ACCOUNT_SERVER_ID_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.NEW_WEBSITE_SERVER_ID_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.OLD_DATABASE_HOST_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.OLD_DATABASE_SERVER_ID_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.OLD_UNIX_ACCOUNT_SERVER_ID_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.OLD_WEBSITE_SERVER_ID_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.RESOURCE_ID_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.REVERTING_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.SERVER_ID_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.SERVICE_ID_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.TE_PARAMS_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.TRANSFER_DATABASES_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.UNIX_ACCOUNT_AND_DATABASE_SENT_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.WEBSITE_SENT_KEY;

@Component
public class AccountTransferService {
    private final RcUserFeignClient rcUserFeignClient;
    private final RcStaffFeignClient rcStaffFeignClient;
    private final BusinessHelper businessHelper;
    private final ProcessingBusinessOperationRepository processingBusinessOperationRepository;
    private final ProcessingBusinessActionRepository processingBusinessActionRepository;

    public AccountTransferService(
            RcUserFeignClient rcUserFeignClient,
            RcStaffFeignClient rcStaffFeignClient,
            BusinessHelper businessHelper,
            ProcessingBusinessOperationRepository processingBusinessOperationRepository,
            ProcessingBusinessActionRepository processingBusinessActionRepository
    ) {
        this.rcUserFeignClient = rcUserFeignClient;
        this.rcStaffFeignClient = rcStaffFeignClient;
        this.businessHelper = businessHelper;
        this.processingBusinessOperationRepository = processingBusinessOperationRepository;
        this.processingBusinessActionRepository = processingBusinessActionRepository;
    }

    public ProcessingBusinessAction startTransfer(SimpleServiceMessage message) {
        String newServerId = (String) message.getParam(SERVER_ID_KEY);
        Boolean transferDatabases = (Boolean) message.getParam(TRANSFER_DATABASES_KEY);

        List<UnixAccount> unixAccounts = (List<UnixAccount>) rcUserFeignClient.getUnixAccounts(message.getAccountId());

        if (unixAccounts == null || unixAccounts.isEmpty()) {
            throw new ParameterValidationException("UnixAccount не найден");
        }

        String oldUnixAccountServerId = unixAccounts.get(0).getServerId();

        AccountTransferRequest accountTransferRequest = new AccountTransferRequest();
        accountTransferRequest.setAccountId(message.getAccountId());
        accountTransferRequest.setOldUnixAccountServerId(oldUnixAccountServerId);
        accountTransferRequest.setNewUnixAccountServerId(newServerId);
        accountTransferRequest.setNewDatabaseServerId(newServerId);
        accountTransferRequest.setNewWebSiteServerId(newServerId);
        accountTransferRequest.setTransferDatabases(transferDatabases != null ? transferDatabases : true);

        return startTransferUnixAccountAndDatabase(accountTransferRequest);
    }

    public void revertTransferUnixAccountAndDatabase(ProcessingBusinessOperation processingBusinessOperation) {
        Boolean reverting = (Boolean) processingBusinessOperation.getParam(REVERTING_KEY);

        if (reverting == null || !reverting) {
            processingBusinessOperation.addParam(REVERTING_KEY, true);
            processingBusinessOperation.addParam(REVERTING_KEY, true);
            processingBusinessOperationRepository.save(processingBusinessOperation);

            //Меняем id местами
            String newUnixAccountServerId = (String) processingBusinessOperation.getParam(OLD_UNIX_ACCOUNT_SERVER_ID_KEY);
            String oldUnixAccountServerId = (String) processingBusinessOperation.getParam(NEW_UNIX_ACCOUNT_SERVER_ID_KEY);
            String newDatabaseServerId = (String) processingBusinessOperation.getParam(OLD_DATABASE_SERVER_ID_KEY);
            String oldDatabaseServerId = (String) processingBusinessOperation.getParam(NEW_DATABASE_SERVER_ID_KEY);

            Boolean transferDatabases = (Boolean) processingBusinessOperation.getParam(TRANSFER_DATABASES_KEY);

            AccountTransferRequest accountTransferRequest = new AccountTransferRequest();
            accountTransferRequest.setAccountId(processingBusinessOperation.getPersonalAccountId());
            accountTransferRequest.setOldUnixAccountServerId(oldUnixAccountServerId);
            accountTransferRequest.setNewUnixAccountServerId(newUnixAccountServerId);
            accountTransferRequest.setOldDatabaseServerId(oldDatabaseServerId);
            accountTransferRequest.setNewDatabaseServerId(newDatabaseServerId);
            accountTransferRequest.setTransferData(false);
            accountTransferRequest.setTransferDatabases(transferDatabases != null ? transferDatabases : true);

            try {
                startTransferUnixAccountAndDatabase(accountTransferRequest);
            } catch (Exception e) {
                e.printStackTrace();

                processingBusinessOperation.setState(State.ERROR);
                processingBusinessOperationRepository.save(processingBusinessOperation);
            }
        }
    }

    public void revertTransferWebSites(ProcessingBusinessOperation processingBusinessOperation) {
        Boolean reverting = (Boolean) processingBusinessOperation.getParam(REVERTING_KEY);
        Boolean webSiteSent = (Boolean) processingBusinessOperation.getParam(WEBSITE_SENT_KEY);

        if (webSiteSent != null && webSiteSent && (reverting == null || !reverting)) {
            processingBusinessOperation.addParam(REVERTING_KEY, true);
            processingBusinessOperationRepository.save(processingBusinessOperation);

            //Меняем id местами
            String newServerId = (String) processingBusinessOperation.getParam(OLD_UNIX_ACCOUNT_SERVER_ID_KEY);
            String oldServerId = (String) processingBusinessOperation.getParam(NEW_UNIX_ACCOUNT_SERVER_ID_KEY);
            String newWebSiteServerId = (String) processingBusinessOperation.getParam(OLD_WEBSITE_SERVER_ID_KEY);
            String oldWebSiteServerId = (String) processingBusinessOperation.getParam(NEW_WEBSITE_SERVER_ID_KEY);

            Boolean transferDatabases = (Boolean) processingBusinessOperation.getParam(TRANSFER_DATABASES_KEY);

            AccountTransferRequest accountTransferRequest = new AccountTransferRequest();
            accountTransferRequest.setAccountId(processingBusinessOperation.getPersonalAccountId());
            accountTransferRequest.setOldUnixAccountServerId(oldServerId);
            accountTransferRequest.setNewUnixAccountServerId(newServerId);
            accountTransferRequest.setOldWebSiteServerId(oldWebSiteServerId);
            accountTransferRequest.setNewWebSiteServerId(newWebSiteServerId);
            accountTransferRequest.setTransferData(false);
            accountTransferRequest.setTransferDatabases(transferDatabases != null ? transferDatabases : true);

            try {
                startTransferWebSites(accountTransferRequest);
            } catch (Exception e) {
                e.printStackTrace();

                processingBusinessOperation.setState(State.ERROR);
                processingBusinessOperationRepository.save(processingBusinessOperation);
            }

            revertTransferUnixAccountAndDatabase(processingBusinessOperation);
        }
    }

    private ProcessingBusinessAction startTransferUnixAccountAndDatabase(AccountTransferRequest accountTransferRequest) {
        String oldDatabaseHost = null, newDatabaseHost = null;

        List<UnixAccount> unixAccounts = (List<UnixAccount>) rcUserFeignClient.getUnixAccounts(accountTransferRequest.getAccountId());

        if (unixAccounts == null || unixAccounts.isEmpty()) {
            throw new ParameterValidationException("UnixAccount не найден");
        }

        ProcessingBusinessAction processingBusinessAction = null;

        for (UnixAccount unixAccount : unixAccounts) {
            SimpleServiceMessage unixAccountMessage = new SimpleServiceMessage();
            unixAccountMessage.setAccountId(accountTransferRequest.getAccountId());
            unixAccountMessage.setOperationIdentity(accountTransferRequest.getOperationId());
            unixAccountMessage.addParam(RESOURCE_ID_KEY, unixAccount.getId());
            unixAccountMessage.addParam(SERVER_ID_KEY, accountTransferRequest.getNewUnixAccountServerId());

            if (accountTransferRequest.isTransferData()) {
                Map<String, Object> teParams = new HashMap<>();

                List<Service> oldServerNginxServices;

                try {
                    oldServerNginxServices = rcStaffFeignClient.getNginxServicesByServerId(accountTransferRequest.getOldUnixAccountServerId());
                } catch (Exception e) {
                    throw new ParameterValidationException("Ошибка при получении сервисов nginx для текущего сервера");
                }

                if (oldServerNginxServices == null || oldServerNginxServices.isEmpty()) {
                    throw new ParameterValidationException("Сервисы nginx не найдены на текущем сервере");
                }

                Service oldNginxService = oldServerNginxServices.get(0);

                String oldNginxHost = oldNginxService.getServiceSockets().get(0).getAddressAsString();

                teParams.put(DATASOURCE_URI_KEY, "rsync://" + oldNginxHost + unixAccount.getHomeDir());

                unixAccountMessage.addParam(TE_PARAMS_KEY, teParams);

                processingBusinessAction = transferUnixAccount(unixAccountMessage);
            } else {
                processingBusinessAction = revertTransferUnixAccount(unixAccountMessage);
            }

            accountTransferRequest.setOperationId(processingBusinessAction.getOperationId());

            ProcessingBusinessOperation processingBusinessOperation = processingBusinessOperationRepository.findOne(processingBusinessAction.getOperationId());
            processingBusinessOperation.addParam(UNIX_ACCOUNT_AND_DATABASE_SENT_KEY, false);
            processingBusinessOperation.addParam(WEBSITE_SENT_KEY, false);
            processingBusinessOperation.addParam(DNS_RECORD_SENT_KEY, false);

            processingBusinessOperationRepository.save(processingBusinessOperation);
        }

        if (accountTransferRequest.isTransferDatabases()) {
            List<DatabaseUser> databaseUsers = rcUserFeignClient.getDatabaseUsers(accountTransferRequest.getAccountId());
            List<Database> databases = (List<Database>) rcUserFeignClient.getDatabases(accountTransferRequest.getAccountId());

            String oldDatabaseServiceId = !databaseUsers.isEmpty() ?
                    databaseUsers.get(0).getServiceId() :
                    (!databases.isEmpty() ? databases.get(0).getServiceId() : null);

            Server oldDatabaseServer = rcStaffFeignClient.getServerByServiceId(
                    oldDatabaseServiceId != null ?
                            oldDatabaseServiceId :
                            accountTransferRequest.getOldUnixAccountServerId()
            );

            if (oldDatabaseServer == null) {
                throw new ParameterValidationException("Старый сервер баз данных не найден");
            }

            accountTransferRequest.setOldDatabaseServerId(oldDatabaseServer.getId());

            List<Service> oldDatabaseServices;

            try {
                oldDatabaseServices = rcStaffFeignClient.getDatabaseServicesByServerId(accountTransferRequest.getOldDatabaseServerId());
            } catch (Exception e) {
                throw new ParameterValidationException("Сервисы старого сервера баз данных не найдены");
            }

            if (oldDatabaseServices == null
                    || oldDatabaseServices.isEmpty()
                    || oldDatabaseServices.get(0).getServiceSockets().isEmpty()) {
                throw new ParameterValidationException("Сервисы старого сервера баз данных пусты");
            }

            Service oldDatabaseService = oldDatabaseServices.get(0);

            oldDatabaseHost = oldDatabaseService.getServiceSockets().get(0).getAddressAsString();
            accountTransferRequest.setOldDatabaseHost(oldDatabaseHost);

            List<Service> newDatabaseServices;

            try {
                newDatabaseServices = rcStaffFeignClient.getDatabaseServicesByServerId(accountTransferRequest.getNewDatabaseServerId());
            } catch (Exception e) {
                throw new ParameterValidationException("Сервисы нового сервера баз данных не найдены");
            }

            if (newDatabaseServices == null
                    || newDatabaseServices.isEmpty()
                    || newDatabaseServices.get(0).getServiceSockets().isEmpty()) {
                throw new ParameterValidationException("Сервисы нового сервера баз данных пусты");
            }

            Service newDatabaseService = newDatabaseServices.get(0);

            newDatabaseHost = newDatabaseService.getServiceSockets().get(0).getAddressAsString();
            accountTransferRequest.setNewDatabaseHost(newDatabaseHost);

            for (DatabaseUser databaseUser : databaseUsers) {
                SimpleServiceMessage databaseUserMessage = new SimpleServiceMessage();
                databaseUserMessage.setAccountId(accountTransferRequest.getAccountId());
                databaseUserMessage.setOperationIdentity(accountTransferRequest.getOperationId());
                databaseUserMessage.addParam(RESOURCE_ID_KEY, databaseUser.getId());
                databaseUserMessage.addParam(SERVICE_ID_KEY, newDatabaseService.getId());

                processingBusinessAction = transferDatabaseUser(databaseUserMessage);
                accountTransferRequest.setOperationId(processingBusinessAction.getOperationId());
            }

            for (Database database : databases) {
                SimpleServiceMessage databaseMessage = new SimpleServiceMessage();
                databaseMessage.setAccountId(accountTransferRequest.getAccountId());
                databaseMessage.setOperationIdentity(accountTransferRequest.getOperationId());
                databaseMessage.addParam(RESOURCE_ID_KEY, database.getId());
                databaseMessage.addParam(SERVICE_ID_KEY, newDatabaseService.getId());

                if (accountTransferRequest.isTransferData()) {
                    Map<String, Object> teParams = new HashMap<>();

                    teParams.put(DATASOURCE_URI_KEY, "mysql://" + accountTransferRequest.getOldDatabaseHost() +
                            "/" + database.getName());

                    databaseMessage.addParam(TE_PARAMS_KEY, teParams);
                }

                processingBusinessAction = transferDatabase(databaseMessage);
                accountTransferRequest.setOperationId(processingBusinessAction.getOperationId());
            }
        }

        ProcessingBusinessOperation processingBusinessOperation = processingBusinessOperationRepository.findOne(processingBusinessAction.getOperationId());
        processingBusinessOperation.addParam(OLD_UNIX_ACCOUNT_SERVER_ID_KEY, accountTransferRequest.getOldUnixAccountServerId());
        processingBusinessOperation.addParam(NEW_UNIX_ACCOUNT_SERVER_ID_KEY, accountTransferRequest.getNewUnixAccountServerId());
        processingBusinessOperation.addParam(OLD_DATABASE_SERVER_ID_KEY, accountTransferRequest.getOldDatabaseServerId());
        processingBusinessOperation.addParam(NEW_DATABASE_SERVER_ID_KEY, accountTransferRequest.getNewDatabaseServerId());
        processingBusinessOperation.addParam(OLD_DATABASE_HOST_KEY, oldDatabaseHost);
        processingBusinessOperation.addParam(NEW_DATABASE_HOST_KEY, newDatabaseHost);
        processingBusinessOperation.addParam(UNIX_ACCOUNT_AND_DATABASE_SENT_KEY, true);

        processingBusinessOperationRepository.save(processingBusinessOperation);

        return processingBusinessAction;
    }

    public void checkOperationAfterUnixAccountAndDatabaseUpdate(ProcessingBusinessOperation processingBusinessOperation) {
        Boolean unixAccountAndDatabaseSent = (Boolean) processingBusinessOperation.getParam(UNIX_ACCOUNT_AND_DATABASE_SENT_KEY);

        List<ProcessingBusinessAction> businessActions = processingBusinessActionRepository.findAllByOperationId(processingBusinessOperation.getId());
        if (unixAccountAndDatabaseSent != null && unixAccountAndDatabaseSent) {
            if (businessActions.stream().noneMatch(processingBusinessAction -> processingBusinessAction.getState() != State.PROCESSED)) {
                String newUnixAccountServerId = (String) processingBusinessOperation.getParam(NEW_UNIX_ACCOUNT_SERVER_ID_KEY);
                String oldUnixAccountServerId = (String) processingBusinessOperation.getParam(OLD_UNIX_ACCOUNT_SERVER_ID_KEY);
                String newDatabaseServerId = (String) processingBusinessOperation.getParam(NEW_DATABASE_SERVER_ID_KEY);
                String oldDatabaseServerId = (String) processingBusinessOperation.getParam(OLD_DATABASE_SERVER_ID_KEY);
                String oldDatabaseHost = (String) processingBusinessOperation.getParam(OLD_DATABASE_HOST_KEY);
                String newDatabaseHost = (String) processingBusinessOperation.getParam(NEW_DATABASE_HOST_KEY);
                Boolean transferDatabases = (Boolean) processingBusinessOperation.getParam(TRANSFER_DATABASES_KEY);

                AccountTransferRequest accountTransferRequest = new AccountTransferRequest();
                accountTransferRequest.setAccountId(processingBusinessOperation.getPersonalAccountId());
                accountTransferRequest.setOperationId(processingBusinessOperation.getId());
                accountTransferRequest.setOldUnixAccountServerId(oldUnixAccountServerId);
                accountTransferRequest.setNewUnixAccountServerId(newUnixAccountServerId);
                accountTransferRequest.setOldDatabaseServerId(oldDatabaseServerId);
                accountTransferRequest.setNewDatabaseServerId(newDatabaseServerId);
                accountTransferRequest.setNewWebSiteServerId(newUnixAccountServerId);
                accountTransferRequest.setTransferDatabases(transferDatabases != null ? transferDatabases : true);
                accountTransferRequest.setOldDatabaseHost(oldDatabaseHost);
                accountTransferRequest.setNewDatabaseHost(newDatabaseHost);

                try {
                    startTransferWebSites(accountTransferRequest);
                } catch (Exception e) {
                    e.printStackTrace();

                    processingBusinessOperation.setState(State.ERROR);
                    processingBusinessOperationRepository.save(processingBusinessOperation);

                    revertTransferUnixAccountAndDatabase(processingBusinessOperation);
                }
            } else if (businessActions.stream().anyMatch(processingBusinessAction -> processingBusinessAction.getState() == State.ERROR)) {
                processingBusinessOperation.setState(State.ERROR);
                processingBusinessOperationRepository.save(processingBusinessOperation);

                revertTransferUnixAccountAndDatabase(processingBusinessOperation);
            }
        }
    }

    public void checkOperationAfterWebSiteUpdate(ProcessingBusinessOperation processingBusinessOperation) {
        Boolean webSiteSent = (Boolean) processingBusinessOperation.getParam(WEBSITE_SENT_KEY);

        List<ProcessingBusinessAction> businessActions = processingBusinessActionRepository.findAllByOperationId(processingBusinessOperation.getId());
        if (webSiteSent != null && webSiteSent) {
            if (businessActions.stream().noneMatch(processingBusinessAction -> processingBusinessAction.getState() != State.PROCESSED)) {
                String newServerId = (String) processingBusinessOperation.getParam(NEW_UNIX_ACCOUNT_SERVER_ID_KEY);
                String oldServerId = (String) processingBusinessOperation.getParam(OLD_UNIX_ACCOUNT_SERVER_ID_KEY);
                String newDatabaseServerId = (String) processingBusinessOperation.getParam(NEW_DATABASE_SERVER_ID_KEY);
                String oldDatabaseServerId = (String) processingBusinessOperation.getParam(OLD_DATABASE_SERVER_ID_KEY);
                String newWebSiteServerId = (String) processingBusinessOperation.getParam(NEW_WEBSITE_SERVER_ID_KEY);
                String oldWebSiteServerId = (String) processingBusinessOperation.getParam(OLD_WEBSITE_SERVER_ID_KEY);
                Boolean transferDatabases = (Boolean) processingBusinessOperation.getParam(TRANSFER_DATABASES_KEY);

                AccountTransferRequest accountTransferRequest = new AccountTransferRequest();
                accountTransferRequest.setAccountId(processingBusinessOperation.getPersonalAccountId());
                accountTransferRequest.setOperationId(processingBusinessOperation.getId());
                accountTransferRequest.setOldUnixAccountServerId(oldServerId);
                accountTransferRequest.setNewUnixAccountServerId(newServerId);
                accountTransferRequest.setOldDatabaseServerId(oldDatabaseServerId);
                accountTransferRequest.setNewDatabaseServerId(newDatabaseServerId);
                accountTransferRequest.setOldWebSiteServerId(oldWebSiteServerId);
                accountTransferRequest.setNewWebSiteServerId(newWebSiteServerId);
                accountTransferRequest.setTransferDatabases(transferDatabases != null ? transferDatabases : true);

                try {
                    startUpdateDNSRecords(accountTransferRequest);
                } catch (Exception e) {
                    e.printStackTrace();

                    processingBusinessOperation.setState(State.ERROR);
                    processingBusinessOperationRepository.save(processingBusinessOperation);

                    revertTransferWebSites(processingBusinessOperation);
                }
            } else if (businessActions.stream().anyMatch(processingBusinessAction -> processingBusinessAction.getState() == State.ERROR)) {
                processingBusinessOperation.setState(State.ERROR);
                processingBusinessOperationRepository.save(processingBusinessOperation);

                revertTransferWebSites(processingBusinessOperation);
            }
        }
    }

    public void finishOperation(ProcessingBusinessOperation processingBusinessOperation) {
        Boolean dnsRecordSent = (Boolean) processingBusinessOperation.getParam(DNS_RECORD_SENT_KEY);

        List<ProcessingBusinessAction> businessActions = processingBusinessActionRepository.findAllByOperationId(processingBusinessOperation.getId());
        if (dnsRecordSent != null
                && dnsRecordSent
                && businessActions.stream().noneMatch(processingBusinessAction -> processingBusinessAction.getState() != State.PROCESSED)) {
            processingBusinessOperation.setState(State.PROCESSED);
            processingBusinessOperationRepository.save(processingBusinessOperation);
        }
    }

    private void startTransferWebSites(AccountTransferRequest accountTransferRequest) {
        List<WebSite> webSites = rcUserFeignClient.getWebSites(accountTransferRequest.getAccountId());

        if (!webSites.isEmpty()) {
            String oldWebSiteServiceId = webSites.get(0).getServiceId();

            Server oldWebSiteServer = rcStaffFeignClient.getServerByServiceId(oldWebSiteServiceId);

            if (oldWebSiteServer == null) {
                throw new ParameterValidationException("Старый веб-сервер не найден");
            }

            accountTransferRequest.setOldWebSiteServerId(oldWebSiteServer.getId());

            List<Service> oldServerWebSiteServices;

            try {
                oldServerWebSiteServices = rcStaffFeignClient.getWebsiteServicesByServerId(accountTransferRequest.getOldWebSiteServerId());
            } catch (Exception e) {
                throw new ParameterValidationException("Ошибка при получении сервисов для вебсайтов для текущего сервера");
            }

            if (oldServerWebSiteServices == null || oldServerWebSiteServices.isEmpty()) {
                throw new ParameterValidationException("Сервисы для вебсайтов не найдены на текущем сервере");
            }

            Map<String, Service> oldServerWebSiteServicesById = oldServerWebSiteServices.stream().collect(Collectors.toMap(Service::getId, s -> s));

            List<Service> newServerWebSiteServices;

            try {
                newServerWebSiteServices = rcStaffFeignClient.getWebsiteServicesByServerId(accountTransferRequest.getNewWebSiteServerId());
            } catch (Exception e) {
                throw new ParameterValidationException("Ошибка при получении сервисов для вебсайтов для нового сервера");
            }

            if (newServerWebSiteServices == null || newServerWebSiteServices.isEmpty()) {
                throw new ParameterValidationException("Сервисы для вебсайтов не найдены на новом сервере");
            }

            //Сначала проверим есть ли все нужные сервисы на новом сервере
            for (WebSite webSite : webSites) {
                Service oldServerWebSiteService = oldServerWebSiteServicesById.get(webSite.getServiceId());

                if (oldServerWebSiteService == null) {
                    throw new ParameterValidationException("Не найден текущий сервис для сайта " + webSite.getId() + " в списке сервисов старого сервера");
                }

                String servicePrefix = oldServerWebSiteService.getName().split("@")[0];

                Service newService = newServerWebSiteServices.stream()
                        .filter(s -> s.getName().split("@")[0].equals(servicePrefix))
                        .findFirst()
                        .orElse(null);

                if (newService == null) {
                    throw new ParameterValidationException("На новом сервере не найден сервис " + servicePrefix);
                }
            }

            ProcessingBusinessAction processingBusinessAction = null;

            for (WebSite webSite : webSites) {
                Service currentService = oldServerWebSiteServicesById.get(webSite.getServiceId());

                if (currentService == null) {
                    throw new ParameterValidationException("Не найден текущий сервис для сайта " + webSite.getId() + " в списке сервисов старого сервера");
                }

                String servicePrefix = currentService.getName().split("@")[0];

                Service newService = newServerWebSiteServices.stream()
                        .filter(s -> s.getName().split("@")[0].equals(servicePrefix))
                        .findFirst()
                        .orElse(null);

                if (newService == null) {
                    throw new ParameterValidationException("На новом сервере не найден сервис " + servicePrefix);
                }

                SimpleServiceMessage webSiteMessage = new SimpleServiceMessage();
                webSiteMessage.setAccountId(accountTransferRequest.getAccountId());
                webSiteMessage.setOperationIdentity(accountTransferRequest.getOperationId());
                webSiteMessage.addParam(RESOURCE_ID_KEY, webSite.getId());
                webSiteMessage.addParam(SERVICE_ID_KEY, newService.getId());

                if (accountTransferRequest.isTransferData()) {
                    Map<String, Object> teParams = new HashMap<>();

                    List<Service> oldServerNginxServices;

                    try {
                        oldServerNginxServices = rcStaffFeignClient.getNginxServicesByServerId(accountTransferRequest.getOldWebSiteServerId());
                    } catch (Exception e) {
                        throw new ParameterValidationException("Ошибка при получении сервисов nginx для текущего сервера");
                    }

                    if (oldServerNginxServices == null || oldServerNginxServices.isEmpty()) {
                        throw new ParameterValidationException("Сервисы nginx не найдены на текущем сервере");
                    }

                    Service oldNginxService = oldServerNginxServices.get(0);

                    String oldNginxHost = oldNginxService.getServiceSockets().get(0).getAddressAsString();

                    teParams.put(DATASOURCE_URI_KEY, "rsync://" + oldNginxHost +
                             webSite.getUnixAccount().getHomeDir()+
                            "/" + webSite.getDocumentRoot());

                    teParams.put(DATA_POSTPROCESSOR_TYPE_KEY, DATA_POSTPROCESSOR_STRING_REPLACE_ACTION);

                    Map<String, String> dataPostprocessorArgs = new HashMap<>();
                    dataPostprocessorArgs.put(DATA_POSTPROCESSOR_STRING_SEARCH_PATTERN_ARG, accountTransferRequest.getOldDatabaseHost());
                    dataPostprocessorArgs.put(DATA_POSTPROCESSOR_STRING_REPLACE_STRING_ARG, accountTransferRequest.getNewDatabaseHost());

                    teParams.put(DATA_POSTPROCESSOR_ARGS_KEY, dataPostprocessorArgs);

                    webSiteMessage.addParam(TE_PARAMS_KEY, teParams);
                    processingBusinessAction = transferWebSite(webSiteMessage);
                } else {
                    processingBusinessAction = revertTransferWebSite(webSiteMessage);
                }

                accountTransferRequest.setOperationId(processingBusinessAction.getOperationId());
            }

            if (processingBusinessAction != null) {
                ProcessingBusinessOperation processingBusinessOperation = processingBusinessOperationRepository.findOne(processingBusinessAction.getOperationId());
                processingBusinessOperation.addParam(OLD_WEBSITE_SERVER_ID_KEY, accountTransferRequest.getOldWebSiteServerId());
                processingBusinessOperation.addParam(NEW_WEBSITE_SERVER_ID_KEY, accountTransferRequest.getNewWebSiteServerId());
                processingBusinessOperation.addParam(WEBSITE_SENT_KEY, true);

                processingBusinessOperationRepository.save(processingBusinessOperation);
            }
        }
    }

    private void startUpdateDNSRecords(AccountTransferRequest accountTransferRequest) {
        List<Service> oldServerNginxServices;

        try {
            oldServerNginxServices = rcStaffFeignClient.getNginxServicesByServerId(accountTransferRequest.getOldWebSiteServerId());
        } catch (Exception e) {
            throw new ParameterValidationException("Ошибка при получении сервисов nginx для текущего сервера");
        }

        if (oldServerNginxServices == null || oldServerNginxServices.isEmpty()) {
            throw new ParameterValidationException("Сервисы nginx не найдены на текущем сервере");
        }

        Service oldNginxService = oldServerNginxServices.get(0);

        String oldNginxHost = oldNginxService.getServiceSockets().get(0).getAddressAsString();

        List<Service> newServerNginxServices;

        try {
            newServerNginxServices = rcStaffFeignClient.getNginxServicesByServerId(accountTransferRequest.getNewWebSiteServerId());
        } catch (Exception e) {
            throw new ParameterValidationException("Ошибка при получении сервисов nginx для нового сервера");
        }

        if (newServerNginxServices == null || newServerNginxServices.isEmpty()) {
            throw new ParameterValidationException("Сервисы nginx не найдены на новом сервере");
        }

        Service newNginxService = newServerNginxServices.get(0);

        String newNginxHost = newNginxService.getServiceSockets().get(0).getAddressAsString();

        List<Domain> domains = rcUserFeignClient.getDomains(accountTransferRequest.getAccountId());

        ProcessingBusinessAction processingBusinessAction = null;

        for (Domain domain : domains) {
            List<DNSResourceRecord> aRecords = domain
                    .getDnsResourceRecords()
                    .stream()
                    .filter(dnsResourceRecord -> dnsResourceRecord.getRrType() == DNSResourceRecordType.A)
                    .collect(Collectors.toList());

            for (DNSResourceRecord dnsResourceRecord : aRecords) {
                if (dnsResourceRecord.getData().equals(oldNginxHost)) {
                    SimpleServiceMessage dnsRecordMessage = new SimpleServiceMessage();
                    dnsRecordMessage.setAccountId(accountTransferRequest.getAccountId());
                    dnsRecordMessage.setOperationIdentity(accountTransferRequest.getOperationId());
                    dnsRecordMessage.addParam(RESOURCE_ID_KEY, String.valueOf(dnsResourceRecord.getRecordId()));
                    dnsRecordMessage.addParam(DATA_KEY, newNginxHost);

                    processingBusinessAction = updateDNSRecord(dnsRecordMessage);
                }
            }
        }

        if (processingBusinessAction != null) {
            ProcessingBusinessOperation processingBusinessOperation = processingBusinessOperationRepository.findOne(processingBusinessAction.getOperationId());
            processingBusinessOperation.addParam(DNS_RECORD_SENT_KEY, true);

            processingBusinessOperationRepository.save(processingBusinessOperation);
        }
    }

    private ProcessingBusinessAction transferUnixAccount(SimpleServiceMessage message) {
        if (message.getOperationIdentity() != null) {
            return businessHelper.buildActionByOperationId(
                    BusinessActionType.UNIX_ACCOUNT_UPDATE_RC,
                    message,
                    message.getOperationIdentity()
            );
        } else {
            return businessHelper.buildActionAndOperation(
                    BusinessOperationType.ACCOUNT_TRANSFER,
                    BusinessActionType.UNIX_ACCOUNT_UPDATE_RC,
                    message
            );
        }
    }

    private ProcessingBusinessAction revertTransferUnixAccount(SimpleServiceMessage message) {
        if (message.getOperationIdentity() != null) {
            return businessHelper.buildActionByOperationId(
                    BusinessActionType.UNIX_ACCOUNT_UPDATE_RC,
                    message,
                    message.getOperationIdentity()
            );
        } else {
            return businessHelper.buildActionAndOperation(
                    BusinessOperationType.ACCOUNT_TRANSFER_REVERT,
                    BusinessActionType.UNIX_ACCOUNT_UPDATE_RC,
                    message
            );
        }
    }

    private ProcessingBusinessAction transferDatabaseUser(SimpleServiceMessage message) {
        if (message.getOperationIdentity() != null) {
            return businessHelper.buildActionByOperationId(
                    BusinessActionType.DATABASE_USER_UPDATE_RC,
                    message,
                    message.getOperationIdentity()
            );
        } else {
            return businessHelper.buildActionAndOperation(
                    BusinessOperationType.ACCOUNT_TRANSFER,
                    BusinessActionType.DATABASE_USER_UPDATE_RC,
                    message
            );
        }
    }

    private ProcessingBusinessAction transferDatabase(SimpleServiceMessage message) {
        if (message.getOperationIdentity() != null) {
            return businessHelper.buildActionByOperationId(
                    BusinessActionType.DATABASE_UPDATE_RC,
                    message,
                    message.getOperationIdentity()
            );
        } else {
            return businessHelper.buildActionAndOperation(
                    BusinessOperationType.ACCOUNT_TRANSFER,
                    BusinessActionType.DATABASE_UPDATE_RC,
                    message
            );
        }
    }

    private ProcessingBusinessAction transferWebSite(SimpleServiceMessage message) {
        if (message.getOperationIdentity() != null) {
            return businessHelper.buildActionByOperationId(
                    BusinessActionType.WEB_SITE_UPDATE_RC,
                    message,
                    message.getOperationIdentity()
            );
        } else {
            return businessHelper.buildActionAndOperation(
                    BusinessOperationType.ACCOUNT_TRANSFER,
                    BusinessActionType.WEB_SITE_UPDATE_RC,
                    message
            );
        }
    }

    private ProcessingBusinessAction revertTransferWebSite(SimpleServiceMessage message) {
        if (message.getOperationIdentity() != null) {
            return businessHelper.buildActionByOperationId(
                    BusinessActionType.WEB_SITE_UPDATE_RC,
                    message,
                    message.getOperationIdentity()
            );
        } else {
            return businessHelper.buildActionAndOperation(
                    BusinessOperationType.ACCOUNT_TRANSFER_REVERT,
                    BusinessActionType.WEB_SITE_UPDATE_RC,
                    message
            );
        }
    }

    private ProcessingBusinessAction updateDNSRecord(SimpleServiceMessage message) {
        if (message.getOperationIdentity() != null) {
            return businessHelper.buildActionByOperationId(
                    BusinessActionType.DNS_RECORD_UPDATE_RC,
                    message,
                    message.getOperationIdentity()
            );
        } else {
            return businessHelper.buildActionAndOperation(
                    BusinessOperationType.ACCOUNT_TRANSFER,
                    BusinessActionType.DNS_RECORD_UPDATE_RC,
                    message
            );
        }
    }
}
