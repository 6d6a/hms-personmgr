package ru.majordomo.hms.personmgr.service;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

import ru.majordomo.hms.personmgr.common.BusinessActionType;
import ru.majordomo.hms.personmgr.common.BusinessOperationType;
import ru.majordomo.hms.personmgr.common.StaffServiceUtils;
import ru.majordomo.hms.personmgr.common.State;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.dto.AccountTransferRequest;
import ru.majordomo.hms.personmgr.exception.InternalApiException;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.exception.ResourceNotFoundException;
import ru.majordomo.hms.personmgr.feign.RcStaffFeignClient;
import ru.majordomo.hms.personmgr.feign.RcUserFeignClient;
import ru.majordomo.hms.personmgr.manager.AccountHistoryManager;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessAction;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessOperation;
import ru.majordomo.hms.personmgr.repository.ProcessingBusinessActionRepository;
import ru.majordomo.hms.personmgr.repository.ProcessingBusinessOperationRepository;
import ru.majordomo.hms.rc.staff.resources.Server;
import ru.majordomo.hms.rc.staff.resources.Service;
import ru.majordomo.hms.rc.user.resources.*;

import javax.annotation.Nonnull;

import static ru.majordomo.hms.personmgr.common.Constants.DATASOURCE_URI_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.DATA_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.DATA_POSTPROCESSOR_ARGS_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.DATA_POSTPROCESSOR_STRING_REPLACE_ACTION;
import static ru.majordomo.hms.personmgr.common.Constants.DATA_POSTPROCESSOR_STRING_REPLACE_STRING_ARG;
import static ru.majordomo.hms.personmgr.common.Constants.DATA_POSTPROCESSOR_STRING_SEARCH_PATTERN_ARG;
import static ru.majordomo.hms.personmgr.common.Constants.DATA_POSTPROCESSOR_TYPE_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.DELAY_MESSAGE_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.DNS_RECORD_SENT_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.NAME_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.NEW_DATABASE_HOST_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.NEW_DATABASE_SERVER_ID_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.NEW_UNIX_ACCOUNT_SERVER_ID_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.NEW_WEBSITE_SERVER_ID_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.OLD_DATABASE_HOST_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.OLD_DATABASE_SERVER_ID_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.OLD_HTTP_PROXY_IP_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.OLD_SERVER_NAME_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.OLD_UNIX_ACCOUNT_SERVER_ID_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.OLD_WEBSITE_SERVER_ID_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.OWNER_NAME_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.PM_PARAM_PREFIX_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.RESOURCE_ID_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.REVERTING_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.SERVER_ID_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.SERVICE_ID_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.TE_PARAMS_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.TRANSFER_DATABASES_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.UNIX_ACCOUNT_AND_DATABASE_SENT_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.WAIT_FOR_DATABASE_UPDATE_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.WAIT_FOR_DATABASE_USER_UPDATE_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.WEBSITE_SENT_KEY;

@Component
public class AccountTransferService {
    private final static Logger log = LoggerFactory.getLogger(AccountTransferService.class);
    private final RcUserFeignClient rcUserFeignClient;
    private final RcStaffFeignClient rcStaffFeignClient;
    private final BusinessHelper businessHelper;
    private final ProcessingBusinessOperationRepository processingBusinessOperationRepository;
    private final ProcessingBusinessActionRepository processingBusinessActionRepository;
    private final AccountHistoryManager history;

    public AccountTransferService(
            RcUserFeignClient rcUserFeignClient,
            RcStaffFeignClient rcStaffFeignClient,
            BusinessHelper businessHelper,
            ProcessingBusinessOperationRepository processingBusinessOperationRepository,
            ProcessingBusinessActionRepository processingBusinessActionRepository,
            AccountHistoryManager history
    ) {
        this.rcUserFeignClient = rcUserFeignClient;
        this.rcStaffFeignClient = rcStaffFeignClient;
        this.businessHelper = businessHelper;
        this.processingBusinessOperationRepository = processingBusinessOperationRepository;
        this.processingBusinessActionRepository = processingBusinessActionRepository;
        this.history = history;
    }

    public ProcessingBusinessAction startTransfer(SimpleServiceMessage message) {
        String newServerId = (String) message.getParam(SERVER_ID_KEY);

        boolean transferDatabases = !Boolean.FALSE.equals(message.getParam(TRANSFER_DATABASES_KEY));

        List<UnixAccount> unixAccounts = (List<UnixAccount>) rcUserFeignClient.getUnixAccounts(message.getAccountId());

        if (unixAccounts == null || unixAccounts.isEmpty()) {
            throw new ParameterValidationException("UnixAccount ???? ????????????");
        }

        String oldUnixAccountServerId = unixAccounts.get(0).getServerId();

        if (newServerId.equals(oldUnixAccountServerId)) {
            throw new ParameterValidationException("?????????????????? ???????????????????? ?????????????? ???? ?????? ???? ????????????");
        }

        AccountTransferRequest accountTransferRequest = new AccountTransferRequest();
        accountTransferRequest.setAccountId(message.getAccountId());
        accountTransferRequest.setUnixAccountId(unixAccounts.get(0).getId());
        accountTransferRequest.setUnixAccountHomeDir(unixAccounts.get(0).getHomeDir());
        accountTransferRequest.setOldUnixAccountServerId(oldUnixAccountServerId);
        accountTransferRequest.setNewUnixAccountServerId(newServerId);
        accountTransferRequest.setNewDatabaseServerId(newServerId);
        accountTransferRequest.setNewWebSiteServerId(newServerId);
        accountTransferRequest.setTransferDatabases(transferDatabases);

        return startTransferUnixAccountAndDatabase(accountTransferRequest);
    }

    public void revertTransfer(ProcessingBusinessOperation processingBusinessOperation) {
        Boolean reverting = (Boolean) processingBusinessOperation.getParam(REVERTING_KEY);

        if (reverting == null || !reverting) {
            history.save(
                    processingBusinessOperation.getPersonalAccountId(),
                    "?????????????????? ???????????? ???? ?????????? ???????????????? ????????????????, ???????????????????????? ?????????? ??????????????????.",
                    "service"
            );

            processingBusinessOperation.addParam(REVERTING_KEY, true);
            processingBusinessOperation.addParam(REVERTING_KEY, true);
            processingBusinessOperationRepository.save(processingBusinessOperation);

            //???????????? id ??????????????
            String newUnixAccountServerId = (String) processingBusinessOperation.getParam(OLD_UNIX_ACCOUNT_SERVER_ID_KEY);
            String oldUnixAccountServerId = (String) processingBusinessOperation.getParam(NEW_UNIX_ACCOUNT_SERVER_ID_KEY);
            String newDatabaseServerId = (String) processingBusinessOperation.getParam(OLD_DATABASE_SERVER_ID_KEY);
            String oldDatabaseServerId = (String) processingBusinessOperation.getParam(NEW_DATABASE_SERVER_ID_KEY);

            String newWebSiteServerId = (String) processingBusinessOperation.getParam(OLD_WEBSITE_SERVER_ID_KEY);

            //???????? ???????????? ???????? ???? ???????????????? ????????????, ???? ?????? ?????????? ???????? null, ?????????? )
            if (newWebSiteServerId == null) {
                newWebSiteServerId = newUnixAccountServerId;
            }

            String oldWebSiteServerId = (String) processingBusinessOperation.getParam(NEW_WEBSITE_SERVER_ID_KEY);

            //???????? ???????????? ???????? ???? ???????????????? ????????????, ???? ?????? ?????????? ???????? null, ?????????? )
            if (oldWebSiteServerId == null) {
                oldWebSiteServerId = oldUnixAccountServerId;
            }

            Boolean transferDatabases = (Boolean) processingBusinessOperation.getParam(TRANSFER_DATABASES_KEY);

            List<UnixAccount> unixAccounts = (List<UnixAccount>) rcUserFeignClient.getUnixAccounts(processingBusinessOperation.getPersonalAccountId());

            if (unixAccounts == null || unixAccounts.isEmpty()) {
                throw new ParameterValidationException("UnixAccount ???? ????????????");
            }

            AccountTransferRequest accountTransferRequest = new AccountTransferRequest();
            accountTransferRequest.setAccountId(processingBusinessOperation.getPersonalAccountId());
            accountTransferRequest.setUnixAccountId(unixAccounts.get(0).getId());
            accountTransferRequest.setUnixAccountHomeDir(unixAccounts.get(0).getHomeDir());
            accountTransferRequest.setOldUnixAccountServerId(oldUnixAccountServerId);
            accountTransferRequest.setNewUnixAccountServerId(newUnixAccountServerId);
            accountTransferRequest.setOldDatabaseServerId(oldDatabaseServerId);
            accountTransferRequest.setNewDatabaseServerId(newDatabaseServerId);
            accountTransferRequest.setOldWebSiteServerId(oldWebSiteServerId);
            accountTransferRequest.setNewWebSiteServerId(newWebSiteServerId);
            accountTransferRequest.setTransferData(false);
            accountTransferRequest.setTransferDatabases(transferDatabases != null ? transferDatabases : true);

            try {
                startTransferUnixAccountAndDatabase(accountTransferRequest);
                startTransferWebSites(accountTransferRequest);
            } catch (Exception e) {
                e.printStackTrace();

                processingBusinessOperation.setState(State.ERROR);
                processingBusinessOperationRepository.save(processingBusinessOperation);
            }
        }
    }

    public void revertTransferOnWebSitesFail(ProcessingBusinessOperation processingBusinessOperation) {
        Boolean webSiteSent = (Boolean) processingBusinessOperation.getParam(WEBSITE_SENT_KEY);

        if (webSiteSent != null && webSiteSent) {
            revertTransfer(processingBusinessOperation);
        }
    }

    private ProcessingBusinessAction startTransferUnixAccountAndDatabase(AccountTransferRequest accountTransferRequest) {
        String oldDatabaseHost = null, newDatabaseHost = null;

        ProcessingBusinessAction processingBusinessAction;

        SimpleServiceMessage unixAccountMessage = new SimpleServiceMessage();
        unixAccountMessage.setAccountId(accountTransferRequest.getAccountId());
        unixAccountMessage.setOperationIdentity(accountTransferRequest.getOperationId());
        unixAccountMessage.addParam(RESOURCE_ID_KEY, accountTransferRequest.getUnixAccountId());
        unixAccountMessage.addParam(SERVER_ID_KEY, accountTransferRequest.getNewUnixAccountServerId());
        unixAccountMessage.addParam(PM_PARAM_PREFIX_KEY + DELAY_MESSAGE_KEY, true);

        if (accountTransferRequest.isTransferData()) {
            Map<String, Object> teParams = new HashMap<>();

            Server oldServer = rcStaffFeignClient.getServerById(accountTransferRequest.getOldUnixAccountServerId());

            if (oldServer == null) {
                throw new ParameterValidationException("???????????? ???????????? ???? ????????????");
            }

            String dataSourceUri = "rsync://" + oldServer.getName() + accountTransferRequest.getUnixAccountHomeDir();
            if (!dataSourceUri.endsWith("/")) {
                dataSourceUri = dataSourceUri + "/";
            }

            String nginxIp = StaffServiceUtils.getFirstNginxIpAddress(oldServer.getServices());
            if (StringUtils.isEmpty(nginxIp)) {
                throw new InternalApiException("???? ?????????????? ?????????? IP-?????????? ?? ?????????????? Nginx");
            }

            teParams.put(DATASOURCE_URI_KEY, dataSourceUri);
            teParams.put(OLD_SERVER_NAME_KEY, oldServer.getName());
            teParams.put(
                    OLD_HTTP_PROXY_IP_KEY,
                    nginxIp
            );

            unixAccountMessage.addParam(TE_PARAMS_KEY, teParams);

            processingBusinessAction = transferUnixAccount(unixAccountMessage);
        } else {
            processingBusinessAction = revertTransferUnixAccount(unixAccountMessage);
        }

        accountTransferRequest.setOperationId(processingBusinessAction.getOperationId());

        ProcessingBusinessOperation processingBusinessOperation = findOperationByIdOrThrow(processingBusinessAction.getOperationId());
        processingBusinessOperation.addParam(UNIX_ACCOUNT_AND_DATABASE_SENT_KEY, false);
        processingBusinessOperation.addParam(WEBSITE_SENT_KEY, false);
        processingBusinessOperation.addParam(DNS_RECORD_SENT_KEY, false);

        processingBusinessOperationRepository.save(processingBusinessOperation);

        if (accountTransferRequest.isTransferDatabases()) {
            Server oldServer = rcStaffFeignClient.getServerById(accountTransferRequest.getOldUnixAccountServerId());

            if (oldServer == null) {
                throw new ParameterValidationException("???????????? ???????????? ???? ????????????");
            }

            List<DatabaseUser> databaseUsers = rcUserFeignClient.getDatabaseUsers(accountTransferRequest.getAccountId());
            List<Database> databases = (List<Database>) rcUserFeignClient.getDatabases(accountTransferRequest.getAccountId());

            String oldDatabaseServiceId = !databaseUsers.isEmpty() ?
                    databaseUsers.get(0).getServiceId() :
                    (!databases.isEmpty() ? databases.get(0).getServiceId() : null);

            Server oldDatabaseServer;
            String oldDatabaseServerId;

            if (oldDatabaseServiceId != null) {
                oldDatabaseServer = rcStaffFeignClient.getServerByServiceId(oldDatabaseServiceId);
                oldDatabaseServerId = oldDatabaseServer.getId();
            } else {
                oldDatabaseServerId = oldServer.getId();
            }

            accountTransferRequest.setOldDatabaseServerId(oldDatabaseServerId);

            Service oldDatabaseService = getDatabaseServiceByServerId(accountTransferRequest.getOldDatabaseServerId());

            oldDatabaseHost = StaffServiceUtils.getFirstIpAddress(oldDatabaseService);
            accountTransferRequest.setOldDatabaseHost(oldDatabaseHost);

            Service newDatabaseService = getDatabaseServiceByServerId(accountTransferRequest.getNewDatabaseServerId());

            newDatabaseHost = StaffServiceUtils.getFirstIpAddress(newDatabaseService);
            accountTransferRequest.setNewDatabaseHost(newDatabaseHost);

            for (DatabaseUser databaseUser : databaseUsers) {
                SimpleServiceMessage databaseUserMessage = new SimpleServiceMessage();
                databaseUserMessage.setAccountId(accountTransferRequest.getAccountId());
                databaseUserMessage.setOperationIdentity(accountTransferRequest.getOperationId());
                databaseUserMessage.addParam(RESOURCE_ID_KEY, databaseUser.getId());
                databaseUserMessage.addParam(SERVICE_ID_KEY, newDatabaseService.getId());
                databaseUserMessage.addParam(PM_PARAM_PREFIX_KEY + DELAY_MESSAGE_KEY, true);

                Map<String, Object> teParams = new HashMap<>();

                String nginxIp = StaffServiceUtils.getFirstNginxIpAddress(oldServer.getServices());
                if (StringUtils.isEmpty(nginxIp)) {
                    throw new InternalApiException("???? ?????????????? ?????????? IP-?????????? ?? ?????????????? Nginx");
                }

                teParams.put(OLD_SERVER_NAME_KEY, oldServer.getName());
                teParams.put(
                        OLD_HTTP_PROXY_IP_KEY,
                        nginxIp
                );

                databaseUserMessage.addParam(TE_PARAMS_KEY, teParams);

                processingBusinessAction = transferDatabaseUser(databaseUserMessage);
                accountTransferRequest.setOperationId(processingBusinessAction.getOperationId());
            }

            for (Database database : databases) {
                SimpleServiceMessage databaseMessage = new SimpleServiceMessage();
                databaseMessage.setAccountId(accountTransferRequest.getAccountId());
                databaseMessage.setOperationIdentity(accountTransferRequest.getOperationId());
                databaseMessage.addParam(RESOURCE_ID_KEY, database.getId());
                databaseMessage.addParam(SERVICE_ID_KEY, newDatabaseService.getId());
                databaseMessage.addParam(PM_PARAM_PREFIX_KEY + DELAY_MESSAGE_KEY, true);

                if (accountTransferRequest.isTransferData()) {
                    Map<String, Object> teParams = new HashMap<>();

                    teParams.put(DATASOURCE_URI_KEY, "mysql://" + accountTransferRequest.getOldDatabaseHost() +
                            "/" + database.getName());

                    String nginxIp = StaffServiceUtils.getFirstNginxIpAddress(oldServer.getServices());
                    if (StringUtils.isEmpty(nginxIp)) {
                        throw new InternalApiException("???? ?????????????? ?????????? IP-?????????? ?? ?????????????? Nginx");
                    }

                    teParams.put(OLD_SERVER_NAME_KEY, oldServer.getName());
                    teParams.put(
                            OLD_HTTP_PROXY_IP_KEY,
                            nginxIp
                    );

                    databaseMessage.addParam(TE_PARAMS_KEY, teParams);
                }

                processingBusinessAction = transferDatabase(databaseMessage);
                accountTransferRequest.setOperationId(processingBusinessAction.getOperationId());
            }

            processingBusinessOperation = processingBusinessOperationRepository
                    .findById(processingBusinessAction.getOperationId())
                    .orElseThrow(() -> new ResourceNotFoundException("???? ?????????????? ????????????????"));

            if (databaseUsers.isEmpty()) {
                processingBusinessOperation.addParam(WAIT_FOR_DATABASE_USER_UPDATE_KEY, false);
            } else {
                processingBusinessOperation.addParam(WAIT_FOR_DATABASE_USER_UPDATE_KEY, true);
            }

            if (databases.isEmpty()) {
                processingBusinessOperation.addParam(WAIT_FOR_DATABASE_UPDATE_KEY, false);
            } else {
                processingBusinessOperation.addParam(WAIT_FOR_DATABASE_UPDATE_KEY, true);
            }

            processingBusinessOperationRepository.save(processingBusinessOperation);
        }

        processingBusinessOperation = findOperationByIdOrThrow(processingBusinessAction.getOperationId());

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

    public void processEventsAmqpForUnixAccountAndDatabaseUpdate(@Nonnull ProcessingBusinessOperation operation, @Nonnull State state) {
        if (state.equals(State.PROCESSED)) {
            checkOperationAfterUnixAccountAndDatabaseUpdate(operation);
        } else if (state.equals(State.ERROR)) {
            operation.setState(State.ERROR);
            processingBusinessOperationRepository.save(operation);

            revertTransfer(operation);
        }
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

                    revertTransfer(processingBusinessOperation);
                }
            } else if (businessActions.stream().anyMatch(processingBusinessAction -> processingBusinessAction.getState() == State.ERROR)) {
                processingBusinessOperation.setState(State.ERROR);
                processingBusinessOperationRepository.save(processingBusinessOperation);

                revertTransfer(processingBusinessOperation);
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
                    startUpdateRedirects(accountTransferRequest);
                } catch (Exception e) {
                    e.printStackTrace();

                    processingBusinessOperation.setState(State.ERROR);
                    processingBusinessOperationRepository.save(processingBusinessOperation);

                    revertTransferOnWebSitesFail(processingBusinessOperation);
                }
            } else if (businessActions.stream().anyMatch(processingBusinessAction -> processingBusinessAction.getState() == State.ERROR)) {
                processingBusinessOperation.setState(State.ERROR);
                processingBusinessOperationRepository.save(processingBusinessOperation);

                revertTransferOnWebSitesFail(processingBusinessOperation);
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

            history.save(
                    processingBusinessOperation.getPersonalAccountId(),
                    "?????????????? ???????????????? ?????????????? ????????????????. ??????-???????????? ????????????????.",
                    "service"
            );
        }
    }

    private void startUpdateRedirects(AccountTransferRequest accountTransferRequest) {
        try {
            Service nginx = getNginxByServerId(accountTransferRequest.getNewWebSiteServerId());
            List<Redirect> redirects = rcUserFeignClient.getRedirects(accountTransferRequest.getAccountId());
            for (Redirect redirect : redirects) {
                SimpleServiceMessage message = new SimpleServiceMessage();
                message.setAccountId(accountTransferRequest.getAccountId());
                message.addParam(RESOURCE_ID_KEY, redirect.getId());
                message.addParam("serviceId", nginx.getId());
                businessHelper.buildAction(BusinessActionType.REDIRECT_UPDATE_RC, message);
            }
        } catch (Exception e) {
            log.error("startUpdateRedirects catch " + e.getClass() + " e.message: " + e.getMessage() + " " + accountTransferRequest.toString());
        }
    }

    private void startTransferWebSites(AccountTransferRequest accountTransferRequest) {
        List<WebSite> webSites = rcUserFeignClient.getWebSites(accountTransferRequest.getAccountId());

        if (!webSites.isEmpty()) {
            String oldWebSiteServiceId = webSites.get(0).getServiceId();

            Server oldWebSiteServer = rcStaffFeignClient.getServerByServiceId(oldWebSiteServiceId);

            if (oldWebSiteServer == null) {
                throw new ParameterValidationException("???????????? ??????-???????????? ???? ????????????");
            }

            accountTransferRequest.setOldWebSiteServerId(oldWebSiteServer.getId());

            List<String> oldServiceIds = webSites.stream().map(WebSite::getServiceId).distinct().collect(Collectors.toList());

            Set<Service> oldServersWebSiteServices = new HashSet<>();

            for (String oldServiceId : oldServiceIds) {
                Server oldServer = rcStaffFeignClient.getServerByServiceId(oldServiceId);

                if (oldServer == null) {
                    throw new ParameterValidationException("???????????? ??????-???????????? ???? ????????????");
                }

                List<Service> oldServerWebSiteServices = getWebSiteServicesByServerId(oldServer.getId());

                oldServersWebSiteServices.addAll(oldServerWebSiteServices);
            }

            if (oldServersWebSiteServices.isEmpty()) {
                throw new ParameterValidationException("?????????????? ?????? ?????????????????? ???? ?????????????? ???? ?????????????? ??????????????");
            }

            Map<String, Service> oldServerWebSiteServicesById = oldServersWebSiteServices
                    .stream()
                    .distinct()
                    .collect(Collectors.toMap(Service::getId, s -> s, (s1, s2) -> s1));

            List<Service> newServerWebSiteServices = getWebSiteServicesByServerId(accountTransferRequest.getNewWebSiteServerId());

            //?????????????? ???????????????? ???????? ???? ?????? ???????????? ?????????????? ???? ?????????? ??????????????
            for (WebSite webSite : webSites) {
                Service oldServerWebSiteService = oldServerWebSiteServicesById.get(webSite.getServiceId());

                if (oldServerWebSiteService == null) {
                    throw new ParameterValidationException("???? ???????????? ?????????????? ???????????? " + webSite.getServiceId() +
                            " ?????? ?????????? " + webSite.getId() + " ?? ???????????? ???????????????? ?????????????? ??????????????");
                }

                String servicePrefix = oldServerWebSiteService.getName().split("@")[0];

                Service newService = newServerWebSiteServices.stream()
                        .filter(s -> s.getName().split("@")[0].equals(servicePrefix))
                        .findFirst()
                        .orElse(null);

                if (newService == null) {
                    throw new ParameterValidationException("???? ?????????? ?????????????? ???? ???????????? ???????????? " + servicePrefix);
                }
            }

            ProcessingBusinessAction processingBusinessAction = null;

            Server oldServer = rcStaffFeignClient.getServerById(accountTransferRequest.getOldWebSiteServerId());

            if (oldServer == null) {
                throw new ParameterValidationException("???????????? ???????????? ???? ????????????");
            }

            for (WebSite webSite : webSites) {
                Service currentService = oldServerWebSiteServicesById.get(webSite.getServiceId());

                if (currentService == null) {
                    throw new ParameterValidationException("???? ???????????? ?????????????? ???????????? " + webSite.getServiceId() +
                            " ?????? ?????????? " + webSite.getId() + " ?? ???????????? ???????????????? ?????????????? ??????????????");
                }

                String servicePrefix = currentService.getName().split("@")[0];

                Service newService = newServerWebSiteServices.stream()
                        .filter(s -> s.getName().split("@")[0].equals(servicePrefix))
                        .findFirst()
                        .orElse(null);

                if (newService == null) {
                    throw new ParameterValidationException("???? ?????????? ?????????????? ???? ???????????? ???????????? " + servicePrefix);
                }

                SimpleServiceMessage webSiteMessage = new SimpleServiceMessage();
                webSiteMessage.setAccountId(accountTransferRequest.getAccountId());
                webSiteMessage.setOperationIdentity(accountTransferRequest.getOperationId());
                webSiteMessage.addParam(RESOURCE_ID_KEY, webSite.getId());
                webSiteMessage.addParam(SERVICE_ID_KEY, newService.getId());

                if (accountTransferRequest.isTransferData()) {
                    Map<String, Object> teParams = new HashMap<>();

                    String dataSourceUri = "rsync://" + oldServer.getName() + webSite.getUnixAccount().getHomeDir()+
                            "/" + webSite.getDocumentRoot();
                    if (!dataSourceUri.endsWith("/")) {
                        dataSourceUri = dataSourceUri + "/";
                    }
                    teParams.put(DATASOURCE_URI_KEY, dataSourceUri);

                    teParams.put(DATA_POSTPROCESSOR_TYPE_KEY, DATA_POSTPROCESSOR_STRING_REPLACE_ACTION);

                    Map<String, String> dataPostprocessorArgs = new HashMap<>();
                    dataPostprocessorArgs.put(DATA_POSTPROCESSOR_STRING_SEARCH_PATTERN_ARG, accountTransferRequest.getOldDatabaseHost());
                    dataPostprocessorArgs.put(DATA_POSTPROCESSOR_STRING_REPLACE_STRING_ARG, accountTransferRequest.getNewDatabaseHost());

                    teParams.put(DATA_POSTPROCESSOR_ARGS_KEY, dataPostprocessorArgs);

                    String nginxIp = StaffServiceUtils.getFirstNginxIpAddress(oldServer.getServices());
                    if (StringUtils.isEmpty(nginxIp)) {
                        throw new InternalApiException("???? ?????????????? ?????????? IP-?????????? ?? ?????????????? Nginx");
                    }

                    teParams.put(OLD_SERVER_NAME_KEY, oldServer.getName());
                    teParams.put(
                            OLD_HTTP_PROXY_IP_KEY,
                            nginxIp
                    );

                    webSiteMessage.addParam(TE_PARAMS_KEY, teParams);
                    processingBusinessAction = transferWebSite(webSiteMessage);
                } else {
                    processingBusinessAction = revertTransferWebSite(webSiteMessage);
                }

                accountTransferRequest.setOperationId(processingBusinessAction.getOperationId());
            }

            if (processingBusinessAction != null) {
                ProcessingBusinessOperation processingBusinessOperation = findOperationByIdOrThrow(processingBusinessAction.getOperationId());
                processingBusinessOperation.addParam(OLD_WEBSITE_SERVER_ID_KEY, accountTransferRequest.getOldWebSiteServerId());
                processingBusinessOperation.addParam(NEW_WEBSITE_SERVER_ID_KEY, accountTransferRequest.getNewWebSiteServerId());
                processingBusinessOperation.addParam(WEBSITE_SENT_KEY, true);

                processingBusinessOperationRepository.save(processingBusinessOperation);
            }
        }
    }

    private void startUpdateDNSRecords(AccountTransferRequest accountTransferRequest) {
        String oldNginxHost = getNginxHostByServerId(accountTransferRequest.getOldWebSiteServerId());
        String newNginxHost = getNginxHostByServerId(accountTransferRequest.getNewWebSiteServerId());

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
                    dnsRecordMessage.addParam(NAME_KEY, String.valueOf(dnsResourceRecord.getName()));
                    dnsRecordMessage.addParam(OWNER_NAME_KEY, String.valueOf(dnsResourceRecord.getOwnerName()));
                    dnsRecordMessage.addParam(DATA_KEY, newNginxHost);

                    processingBusinessAction = updateDNSRecord(dnsRecordMessage);
                }
            }
        }

        ProcessingBusinessOperation processingBusinessOperation = findOperationByIdOrThrow(accountTransferRequest.getOperationId());
        processingBusinessOperation.addParam(DNS_RECORD_SENT_KEY, true);

        if (processingBusinessAction == null) {
            processingBusinessOperation.setState(State.PROCESSED);

            history.save(
                    processingBusinessOperation.getPersonalAccountId(),
                    "?????????????? ???????????????? ?????????????? ????????????????. ??????-???????????? ????????????????.",
                    "service"
            );
        }

        processingBusinessOperationRepository.save(processingBusinessOperation);
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

    @Nonnull
    private String getNginxHostByServerId(String serverId) {
        Service oldNginxService = getNginxByServerId(serverId);

        return StaffServiceUtils.getFirstIpAddress(oldNginxService);
    }

    @Nonnull
    private Service getNginxByServerId(String serverId) {
        List<Service> oldServerNginxServices;

        try {
            oldServerNginxServices = rcStaffFeignClient.getNginxServicesByServerId(serverId);
        } catch (Exception e) {
            e.printStackTrace();
            throw new ParameterValidationException("???????????? ?????? ?????????????????? ???????????????? nginx ?????? ???????????????? ??????????????");
        }

        if (oldServerNginxServices == null || oldServerNginxServices.isEmpty()
                || oldServerNginxServices.get(0) == null) {
            throw new ParameterValidationException("?????????????? nginx ???? ?????????????? ???? ?????????????? ??????????????");
        }

        return oldServerNginxServices.get(0);
    }

    @Nonnull
    private Service getDatabaseServiceByServerId(String serverId) {
        List<Service> databaseServices;

        try {
            databaseServices = rcStaffFeignClient.getDatabaseServicesByServerId(serverId);
        } catch (Exception e) {
            throw new ParameterValidationException("???? ?????????????? ?????????????? ?????? ???????????? ?????? ?????????????? " + serverId);
        }

        if (databaseServices == null || databaseServices.isEmpty() || databaseServices.get(0) == null
                || databaseServices.get(0).getSockets().isEmpty()) {
            throw new ParameterValidationException("???????????? ???????????????? ?????? ???????????? ?????? ?????????????? " + serverId + " ????????");
        }

        return databaseServices.get(0);
    }

    private List<Service> getWebSiteServicesByServerId(String serverId) {
        List<Service> oldServerWebSiteServices;

        try {
            oldServerWebSiteServices = rcStaffFeignClient.getWebsiteServicesByServerId(serverId);
        } catch (Exception e) {
            e.printStackTrace();
            throw new ParameterValidationException("???????????? ?????? ?????????????????? ???????????????? ?????? ?????????????????? ?????? ?????? ?????????????? " + serverId);
        }

        if (oldServerWebSiteServices == null || oldServerWebSiteServices.isEmpty()) {
            throw new ParameterValidationException("???????????? ???????????????? ?????????????????? ?????? ?????????????? " + serverId + " ????????");
        }

        return oldServerWebSiteServices;
    }

    private ProcessingBusinessOperation findOperationByIdOrThrow(String id) {
        return processingBusinessOperationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("???? ?????????????? ???????????????? ?? id " + id));
    }

}
