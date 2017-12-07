package ru.majordomo.hms.personmgr.service;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ru.majordomo.hms.personmgr.common.BusinessActionType;
import ru.majordomo.hms.personmgr.common.BusinessOperationType;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessAction;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessOperation;
import ru.majordomo.hms.personmgr.repository.ProcessingBusinessOperationRepository;
import ru.majordomo.hms.rc.staff.resources.Service;
import ru.majordomo.hms.rc.user.resources.Database;
import ru.majordomo.hms.rc.user.resources.DatabaseUser;
import ru.majordomo.hms.rc.user.resources.UnixAccount;
import ru.majordomo.hms.rc.user.resources.WebSite;

import static ru.majordomo.hms.personmgr.common.Constants.NEW_DATABASE_HOST_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.NEW_SERVER_ID_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.OLD_DATABASE_HOST_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.OLD_SERVER_ID_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.RESOURCE_ID_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.SERVER_ID_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.SERVICE_ID_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.TRANSFER_DATABASES_KEY;

@Component
public class AccountTransferService {
    private final RcUserFeignClient rcUserFeignClient;
    private final RcStaffFeignClient rcStaffFeignClient;
    private final BusinessHelper businessHelper;
    private final ProcessingBusinessOperationRepository processingBusinessOperationRepository;

    public AccountTransferService(
            RcUserFeignClient rcUserFeignClient,
            RcStaffFeignClient rcStaffFeignClient,
            BusinessHelper businessHelper,
            ProcessingBusinessOperationRepository processingBusinessOperationRepository
    ) {
        this.rcUserFeignClient = rcUserFeignClient;
        this.rcStaffFeignClient = rcStaffFeignClient;
        this.businessHelper = businessHelper;
        this.processingBusinessOperationRepository = processingBusinessOperationRepository;
    }

    public ProcessingBusinessAction startTransfer(SimpleServiceMessage message) {
        String newServerId = (String) message.getParam(SERVER_ID_KEY);

        List<Service> newDatabaseServices;

        try {
            newDatabaseServices = rcStaffFeignClient.getDatabaseServicesByServerId(newServerId);
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

        List<UnixAccount> unixAccounts = (List<UnixAccount>) rcUserFeignClient.getUnixAccounts(message.getAccountId());

        if (unixAccounts == null || unixAccounts.isEmpty()) {
            throw new ParameterValidationException("UnixAccount не найден");
        }

        String oldServerId = unixAccounts.get(0).getServerId();

        List<Service> oldDatabaseServices;

        try {
            oldDatabaseServices = rcStaffFeignClient.getDatabaseServicesByServerId(oldServerId);
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

        ProcessingBusinessAction processingBusinessAction = null;

        for (UnixAccount unixAccount : unixAccounts) {
            SimpleServiceMessage unixAccountMessage = new SimpleServiceMessage();
            unixAccountMessage.setAccountId(message.getAccountId());
            unixAccountMessage.setOperationIdentity(message.getOperationIdentity());
            unixAccountMessage.setActionIdentity(message.getActionIdentity());
            unixAccountMessage.addParam(RESOURCE_ID_KEY, unixAccount.getId());
            unixAccountMessage.addParam(SERVER_ID_KEY, newServerId);

            processingBusinessAction = transferUnixAccount(unixAccountMessage);
            message.setOperationIdentity(processingBusinessAction.getOperationId());
        }

        if (processingBusinessAction == null) {
            throw new ParameterValidationException("Не была отправлена заявка на обновление UnixAccount");
        }

        if ((boolean) message.getParam(TRANSFER_DATABASES_KEY)) {


            List<DatabaseUser> databaseUsers = rcUserFeignClient.getDatabaseUsers(message.getAccountId());

            for (DatabaseUser databaseUser : databaseUsers) {
                SimpleServiceMessage databaseUserMessage = new SimpleServiceMessage();
                databaseUserMessage.setAccountId(message.getAccountId());
                databaseUserMessage.setOperationIdentity(message.getOperationIdentity());
                databaseUserMessage.setActionIdentity(message.getActionIdentity());
                databaseUserMessage.addParam(RESOURCE_ID_KEY, databaseUser.getId());
                databaseUserMessage.addParam(SERVICE_ID_KEY, newDatabaseService.getId());

                processingBusinessAction = transferDatabaseUser(databaseUserMessage);
                message.setOperationIdentity(processingBusinessAction.getOperationId());
            }

            List<Database> databases = (List<Database>) rcUserFeignClient.getDatabases(message.getAccountId());

            for (Database database : databases) {
                SimpleServiceMessage databaseMessage = new SimpleServiceMessage();
                databaseMessage.setAccountId(message.getAccountId());
                databaseMessage.setOperationIdentity(message.getOperationIdentity());
                databaseMessage.setActionIdentity(message.getActionIdentity());
                databaseMessage.addParam(RESOURCE_ID_KEY, database.getId());
                databaseMessage.addParam(SERVICE_ID_KEY, newDatabaseService.getId());

                processingBusinessAction = transferDatabase(databaseMessage);
                message.setOperationIdentity(processingBusinessAction.getOperationId());
            }
        }

        ProcessingBusinessOperation processingBusinessOperation = processingBusinessOperationRepository.findOne(processingBusinessAction.getOperationId());
        processingBusinessOperation.addParam(OLD_SERVER_ID_KEY, unixAccounts.get(0).getServerId());
        processingBusinessOperation.addParam(NEW_SERVER_ID_KEY, newServerId);
        processingBusinessOperation.addParam(OLD_DATABASE_HOST_KEY, oldDatabaseHost);
        processingBusinessOperation.addParam(NEW_DATABASE_HOST_KEY, newDatabaseHost);

        processingBusinessOperationRepository.save(processingBusinessOperation);

        return processingBusinessAction;
    }

    public ProcessingBusinessAction startTransferWebSites(SimpleServiceMessage message) {
        ProcessingBusinessOperation processingBusinessOperation = processingBusinessOperationRepository.findOne(message.getOperationIdentity());
        String oldServerId = (String) processingBusinessOperation.getParam(OLD_SERVER_ID_KEY);
        String newServerId = (String) processingBusinessOperation.getParam(NEW_SERVER_ID_KEY);

        List<WebSite> webSites = rcUserFeignClient.getWebSites(message.getAccountId());

        List<Service> oldServerWebSiteServices;

        try {
            oldServerWebSiteServices = rcStaffFeignClient.getWebsiteServicesByServerId(oldServerId);
        } catch (Exception e) {
            throw new ParameterValidationException("Ошибка при получении сервисов для вебсайтов для текущего сервера");
        }

        if (oldServerWebSiteServices == null || oldServerWebSiteServices.isEmpty()) {
            throw new ParameterValidationException("Сервисы для вебсайтов не найдены на текущем сервере");
        }

        Map<String, Service> oldServerWebSiteServicesById = oldServerWebSiteServices.stream().collect(Collectors.toMap(Service::getId, s -> s));

        List<Service> newServerWebSiteServices;

        try {
            newServerWebSiteServices = rcStaffFeignClient.getWebsiteServicesByServerId(newServerId);
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

        ProcessingBusinessAction processingBusinessAction = null;

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
            webSiteMessage.setAccountId(message.getAccountId());
            webSiteMessage.setOperationIdentity(message.getOperationIdentity());
            webSiteMessage.setActionIdentity(message.getActionIdentity());
            webSiteMessage.addParam(RESOURCE_ID_KEY, webSite.getId());
            webSiteMessage.addParam(SERVICE_ID_KEY, newService.getId());

            processingBusinessAction = transferWebSite(webSiteMessage);
            message.setOperationIdentity(processingBusinessAction.getOperationId());
        }

        return processingBusinessAction;
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
}
