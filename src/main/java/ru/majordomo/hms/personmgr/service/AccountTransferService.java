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
import static ru.majordomo.hms.personmgr.common.Constants.NEW_DATABASE_HOST_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.NEW_SERVER_ID_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.OLD_DATABASE_HOST_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.OLD_SERVER_ID_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.RESOURCE_ID_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.REVERTING_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.SERVER_ID_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.SERVICE_ID_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.TE_PARAMS_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.TRANSFER_DATABASES_KEY;

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

        String oldServerId = unixAccounts.get(0).getServerId();

        AccountTransferRequest accountTransferRequest = new AccountTransferRequest();
        accountTransferRequest.setAccountId(message.getAccountId());
        accountTransferRequest.setOldServerId(oldServerId);
        accountTransferRequest.setNewServerId(newServerId);
        accountTransferRequest.setTransferDatabases(transferDatabases != null ? transferDatabases : true);

        return startTransferUnixAccountAndDatabase(accountTransferRequest);
    }

    public void revertTransferUnixAccountAndDatabase(ProcessingBusinessOperation processingBusinessOperation) {
        if (processingBusinessOperation.getParam(REVERTING_KEY) != null) {
            processingBusinessOperation.addParam(REVERTING_KEY, true);
            processingBusinessOperationRepository.save(processingBusinessOperation);

            //Меняем id местами
            String newServerId = (String) processingBusinessOperation.getParam(OLD_SERVER_ID_KEY);
            String oldServerId = (String) processingBusinessOperation.getParam(NEW_SERVER_ID_KEY);

            Boolean transferDatabases = (Boolean) processingBusinessOperation.getParam(TRANSFER_DATABASES_KEY);

            AccountTransferRequest accountTransferRequest = new AccountTransferRequest();
            accountTransferRequest.setAccountId(processingBusinessOperation.getPersonalAccountId());
            accountTransferRequest.setOldServerId(oldServerId);
            accountTransferRequest.setNewServerId(newServerId);
            accountTransferRequest.setTransferData(false);
            accountTransferRequest.setTransferDatabases(transferDatabases != null ? transferDatabases : true);

            startTransferUnixAccountAndDatabase(accountTransferRequest);
        }
    }

    public void revertTransferWebSites(ProcessingBusinessOperation processingBusinessOperation) {
        if (processingBusinessOperation.getParam(REVERTING_KEY) != null) {
            processingBusinessOperation.addParam(REVERTING_KEY, true);
            processingBusinessOperationRepository.save(processingBusinessOperation);

            //Меняем id местами
            String newServerId = (String) processingBusinessOperation.getParam(OLD_SERVER_ID_KEY);
            String oldServerId = (String) processingBusinessOperation.getParam(NEW_SERVER_ID_KEY);

            Boolean transferDatabases = (Boolean) processingBusinessOperation.getParam(TRANSFER_DATABASES_KEY);

            AccountTransferRequest accountTransferRequest = new AccountTransferRequest();
            accountTransferRequest.setAccountId(processingBusinessOperation.getPersonalAccountId());
            accountTransferRequest.setOldServerId(oldServerId);
            accountTransferRequest.setNewServerId(newServerId);
            accountTransferRequest.setTransferData(false);
            accountTransferRequest.setTransferDatabases(transferDatabases != null ? transferDatabases : true);

            startTransferWebSites(accountTransferRequest);
        }
    }

    private ProcessingBusinessAction startTransferUnixAccountAndDatabase(AccountTransferRequest accountTransferRequest) {
        List<Service> newDatabaseServices;

        try {
            newDatabaseServices = rcStaffFeignClient.getDatabaseServicesByServerId(accountTransferRequest.getNewServerId());
        } catch (Exception e) {
            throw new ParameterValidationException("Новый сервер баз данных не найден");
        }

        if (newDatabaseServices == null
                || newDatabaseServices.isEmpty()
                || newDatabaseServices.get(0).getServiceSockets().isEmpty()) {
            throw new ParameterValidationException("Новый сервер баз данных не найден");
        }

        Service newDatabaseService = newDatabaseServices.get(0);

        String newDatabaseHost = newDatabaseService.getServiceSockets().get(0).getAddressAsString();
        accountTransferRequest.setNewDatabaseHost(newDatabaseHost);

        List<UnixAccount> unixAccounts = (List<UnixAccount>) rcUserFeignClient.getUnixAccounts(accountTransferRequest.getAccountId());

        if (unixAccounts == null || unixAccounts.isEmpty()) {
            throw new ParameterValidationException("UnixAccount не найден");
        }

        List<Service> oldDatabaseServices;

        try {
            oldDatabaseServices = rcStaffFeignClient.getDatabaseServicesByServerId(accountTransferRequest.getOldServerId());
        } catch (Exception e) {
            throw new ParameterValidationException("Старый сервер баз данных не найден");
        }

        if (oldDatabaseServices == null
                || oldDatabaseServices.isEmpty()
                || oldDatabaseServices.get(0).getServiceSockets().isEmpty()) {
            throw new ParameterValidationException("Старый сервер баз данных не найден");
        }

        Service oldDatabaseService = oldDatabaseServices.get(0);

        String oldDatabaseHost = oldDatabaseService.getServiceSockets().get(0).getAddressAsString();
        accountTransferRequest.setOldDatabaseHost(oldDatabaseHost);

        ProcessingBusinessAction processingBusinessAction = null;

        for (UnixAccount unixAccount : unixAccounts) {
            SimpleServiceMessage unixAccountMessage = new SimpleServiceMessage();
            unixAccountMessage.setAccountId(accountTransferRequest.getAccountId());
            unixAccountMessage.setOperationIdentity(accountTransferRequest.getOperationId());
            unixAccountMessage.addParam(RESOURCE_ID_KEY, unixAccount.getId());
            unixAccountMessage.addParam(SERVER_ID_KEY, accountTransferRequest.getNewServerId());

            if (accountTransferRequest.isTransferData()) {
                Map<String, Object> teParams = new HashMap<>();

                List<Service> oldServerNginxServices;

                try {
                    oldServerNginxServices = rcStaffFeignClient.getNginxServicesByServerId(accountTransferRequest.getOldServerId());
                } catch (Exception e) {
                    throw new ParameterValidationException("Ошибка при получении сервисов nginx для текущего сервера");
                }

                if (oldServerNginxServices == null || oldServerNginxServices.isEmpty()) {
                    throw new ParameterValidationException("Сервисы nginx не найдены на текущем сервере");
                }

                Service oldNginxService = oldServerNginxServices.get(0);

                String oldNginxHost = oldNginxService.getServiceSockets().get(0).getAddressAsString();

                teParams.put(DATASOURCE_URI_KEY, "rsync://" + oldNginxHost +
                        "/" + unixAccount.getHomeDir());

                unixAccountMessage.addParam(TE_PARAMS_KEY, teParams);

                processingBusinessAction = transferUnixAccount(unixAccountMessage);
            } else {
                processingBusinessAction = revertTransferUnixAccount(unixAccountMessage);
            }

            accountTransferRequest.setOperationId(processingBusinessAction.getOperationId());
        }

        if (processingBusinessAction == null) {
            throw new ParameterValidationException("Не была отправлена заявка на обновление UnixAccount");
        }

        if (accountTransferRequest.isTransferDatabases()) {
            List<DatabaseUser> databaseUsers = rcUserFeignClient.getDatabaseUsers(accountTransferRequest.getAccountId());

            for (DatabaseUser databaseUser : databaseUsers) {
                SimpleServiceMessage databaseUserMessage = new SimpleServiceMessage();
                databaseUserMessage.setAccountId(accountTransferRequest.getAccountId());
                databaseUserMessage.setOperationIdentity(accountTransferRequest.getOperationId());
                databaseUserMessage.addParam(RESOURCE_ID_KEY, databaseUser.getId());
                databaseUserMessage.addParam(SERVICE_ID_KEY, newDatabaseService.getId());

                processingBusinessAction = transferDatabaseUser(databaseUserMessage);
                accountTransferRequest.setOperationId(processingBusinessAction.getOperationId());
            }

            List<Database> databases = (List<Database>) rcUserFeignClient.getDatabases(accountTransferRequest.getAccountId());

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
        processingBusinessOperation.addParam(OLD_SERVER_ID_KEY, unixAccounts.get(0).getServerId());
        processingBusinessOperation.addParam(NEW_SERVER_ID_KEY, accountTransferRequest.getNewServerId());
        processingBusinessOperation.addParam(OLD_DATABASE_HOST_KEY, oldDatabaseHost);
        processingBusinessOperation.addParam(NEW_DATABASE_HOST_KEY, newDatabaseHost);

        processingBusinessOperationRepository.save(processingBusinessOperation);

        return processingBusinessAction;
    }

    public void checkOperationAfterUnixAccountAndDatabaseUpdate(ProcessingBusinessOperation processingBusinessOperation) {
        List<ProcessingBusinessAction> businessActions = processingBusinessActionRepository.findAllByOperationId(processingBusinessOperation.getId());
        if (businessActions.stream().noneMatch(processingBusinessAction -> processingBusinessAction.getState() != State.PROCESSED)) {
            String newServerId = (String) processingBusinessOperation.getParam(NEW_SERVER_ID_KEY);
            String oldServerId = (String) processingBusinessOperation.getParam(OLD_SERVER_ID_KEY);
            String oldDatabaseHost = (String) processingBusinessOperation.getParam(OLD_DATABASE_HOST_KEY);
            String newDatabaseHost = (String) processingBusinessOperation.getParam(NEW_DATABASE_HOST_KEY);
            Boolean transferDatabases = (Boolean) processingBusinessOperation.getParam(TRANSFER_DATABASES_KEY);

            AccountTransferRequest accountTransferRequest = new AccountTransferRequest();
            accountTransferRequest.setAccountId(processingBusinessOperation.getPersonalAccountId());
            accountTransferRequest.setOldServerId(oldServerId);
            accountTransferRequest.setNewServerId(newServerId);
            accountTransferRequest.setTransferDatabases(transferDatabases != null ? transferDatabases : true);
            accountTransferRequest.setOldDatabaseHost(oldDatabaseHost);
            accountTransferRequest.setNewDatabaseHost(newDatabaseHost);

            try {
                startTransferWebSites(accountTransferRequest);
            } catch (Exception e) {
                e.printStackTrace();
                revertTransferUnixAccountAndDatabase(processingBusinessOperation);
            }
        }
    }

    public void checkOperationAfterWebSiteUpdate(ProcessingBusinessOperation processingBusinessOperation) {
        List<ProcessingBusinessAction> businessActions = processingBusinessActionRepository.findAllByOperationId(processingBusinessOperation.getId());
        if (businessActions.stream().noneMatch(processingBusinessAction -> processingBusinessAction.getState() != State.PROCESSED)) {
            String newServerId = (String) processingBusinessOperation.getParam(NEW_SERVER_ID_KEY);
            String oldServerId = (String) processingBusinessOperation.getParam(OLD_SERVER_ID_KEY);
            Boolean transferDatabases = (Boolean) processingBusinessOperation.getParam(TRANSFER_DATABASES_KEY);

            AccountTransferRequest accountTransferRequest = new AccountTransferRequest();
            accountTransferRequest.setAccountId(processingBusinessOperation.getPersonalAccountId());
            accountTransferRequest.setOldServerId(oldServerId);
            accountTransferRequest.setNewServerId(newServerId);
            accountTransferRequest.setTransferDatabases(transferDatabases != null ? transferDatabases : true);

            try {
                startUpdateDNSRecords(accountTransferRequest);
            } catch (Exception e) {
                e.printStackTrace();
                revertTransferUnixAccountAndDatabase(processingBusinessOperation);
                revertTransferWebSites(processingBusinessOperation);
            }
        }
    }

    public void finishOperation(ProcessingBusinessOperation processingBusinessOperation) {
        List<ProcessingBusinessAction> businessActions = processingBusinessActionRepository.findAllByOperationId(processingBusinessOperation.getId());
        if (businessActions.stream().noneMatch(processingBusinessAction -> processingBusinessAction.getState() != State.PROCESSED)) {
            processingBusinessOperation.setState(State.PROCESSED);
            processingBusinessOperationRepository.save(processingBusinessOperation);
        }
    }

    private void startTransferWebSites(AccountTransferRequest accountTransferRequest) {
        List<WebSite> webSites = rcUserFeignClient.getWebSites(accountTransferRequest.getAccountId());

        List<Service> oldServerWebSiteServices;

        try {
            oldServerWebSiteServices = rcStaffFeignClient.getWebsiteServicesByServerId(accountTransferRequest.getOldServerId());
        } catch (Exception e) {
            throw new ParameterValidationException("Ошибка при получении сервисов для вебсайтов для текущего сервера");
        }

        if (oldServerWebSiteServices == null || oldServerWebSiteServices.isEmpty()) {
            throw new ParameterValidationException("Сервисы для вебсайтов не найдены на текущем сервере");
        }

        Map<String, Service> oldServerWebSiteServicesById = oldServerWebSiteServices.stream().collect(Collectors.toMap(Service::getId, s -> s));

        List<Service> newServerWebSiteServices;

        try {
            newServerWebSiteServices = rcStaffFeignClient.getWebsiteServicesByServerId(accountTransferRequest.getNewServerId());
        } catch (Exception e) {
            throw new ParameterValidationException("Ошибка при получении сервисов для вебсайтов для нового сервера");
        }

        if (newServerWebSiteServices == null || newServerWebSiteServices.isEmpty()) {
            throw new ParameterValidationException("Сервисы для вебсайтов не найдены на новом сервере");
        }

        //Сначала проверим есть ли все нужные сервисы на новом сервере
        for (WebSite webSite : webSites) {
            Service currentService = oldServerWebSiteServicesById.get(webSite.getServiceId());

            String servicePrefix = currentService.getName().split("@")[0];

            Service newService = newServerWebSiteServices.stream()
                    .filter(s -> s.getName().split("@")[0].equals(servicePrefix))
                    .findFirst()
                    .orElse(null);

            if (newService == null) {
                throw new ParameterValidationException("На новом сервере не найден сервис " + servicePrefix);
            }
        }

        ProcessingBusinessAction processingBusinessAction;

        for (WebSite webSite : webSites) {
            Service currentService = oldServerWebSiteServicesById.get(webSite.getServiceId());

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
                    oldServerNginxServices = rcStaffFeignClient.getNginxServicesByServerId(accountTransferRequest.getOldServerId());
                } catch (Exception e) {
                    throw new ParameterValidationException("Ошибка при получении сервисов nginx для текущего сервера");
                }

                if (oldServerNginxServices == null || oldServerNginxServices.isEmpty()) {
                    throw new ParameterValidationException("Сервисы nginx не найдены на текущем сервере");
                }

                Service oldNginxService = oldServerNginxServices.get(0);

                String oldNginxHost = oldNginxService.getServiceSockets().get(0).getAddressAsString();

                teParams.put(DATASOURCE_URI_KEY, "rsync://" + oldNginxHost +
                        "/" + webSite.getUnixAccount().getHomeDir()+
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
    }

    private void startUpdateDNSRecords(AccountTransferRequest accountTransferRequest) {
        List<Service> oldServerNginxServices;

        try {
            oldServerNginxServices = rcStaffFeignClient.getNginxServicesByServerId(accountTransferRequest.getOldServerId());
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
            newServerNginxServices = rcStaffFeignClient.getNginxServicesByServerId(accountTransferRequest.getNewServerId());
        } catch (Exception e) {
            throw new ParameterValidationException("Ошибка при получении сервисов nginx для нового сервера");
        }

        if (newServerNginxServices == null || newServerNginxServices.isEmpty()) {
            throw new ParameterValidationException("Сервисы nginx не найдены на новом сервере");
        }

        Service newNginxService = newServerNginxServices.get(0);

        String newNginxHost = newNginxService.getServiceSockets().get(0).getAddressAsString();

        List<Domain> domains = rcUserFeignClient.getDomains(accountTransferRequest.getAccountId());

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
                    dnsRecordMessage.addParam(RESOURCE_ID_KEY, dnsResourceRecord.getId());
                    dnsRecordMessage.addParam(DATA_KEY, newNginxHost);

                    updateDNSRecord(dnsRecordMessage);
                }
            }
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
