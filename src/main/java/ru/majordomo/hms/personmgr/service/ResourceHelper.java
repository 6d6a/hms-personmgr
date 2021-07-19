package ru.majordomo.hms.personmgr.service;

import feign.FeignException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import ru.majordomo.hms.personmgr.common.BusinessActionType;
import ru.majordomo.hms.personmgr.common.BusinessOperationType;
import ru.majordomo.hms.personmgr.common.Utils;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.event.account.SwitchAccountResourcesEvent;
import ru.majordomo.hms.personmgr.exception.ResourceIsLockedException;
import ru.majordomo.hms.personmgr.feign.RcUserFeignClient;
import ru.majordomo.hms.personmgr.manager.AccountHistoryManager;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.manager.PlanManager;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.common.Constants;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessAction;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessOperation;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.rc.user.resources.*;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
@Slf4j
@AllArgsConstructor
@ParametersAreNonnullByDefault
public class ResourceHelper {
    private final RcUserFeignClient rcUserFeignClient;
    private final BusinessHelper businessHelper;
    private final AccountHistoryManager history;
    private final DedicatedAppServiceHelper dedicatedAppServiceHelper;
    private final ApplicationEventPublisher publisher;
    private final PersonalAccountManager personalAccountManager;
    private final PlanManager planManager;
    private final AccountServiceHelper accountServiceHelper;

    public enum SwitchAccountResourcesStage {
        CREATED_STAGE,
        FIRST_PREPARE_STAGE,
        /** сообщения для unix-аккаунтов и доменов отправлены, ожидает завершения */
        FIRST_SENT_ASYNC_STAGE,
        SECOND_PREPARE_ASYNC_STAGE,
        /** сообщения для остальных ресурсов отправлены, ожидает завершения */
        SECOND_SENT_ASYNC_STAGE,
        FINISH_STAGE
    }

    public List<Domain> getDomains(PersonalAccount account) {
        try {
            return rcUserFeignClient.getDomains(account.getId());
        } catch (Exception e) {
            e.printStackTrace();
            log.error("with account {} on getDomains catch e {} message {}", account.getId(), e.getClass(), e.getMessage());
            return new ArrayList<>();
        }
    }

    public void switchAntiSpamForMailboxes(String accountId, Boolean state) {
        Collection<Mailbox> mailboxes = rcUserFeignClient.getMailboxes(accountId);

        for (Mailbox mailbox : mailboxes) {
            SimpleServiceMessage message = new SimpleServiceMessage();
            message.setParams(new HashMap<>());
            message.setAccountId(accountId);
            message.addParam("resourceId", mailbox.getId());
            message.addParam("antiSpamEnabled", state);

            businessHelper.buildAction(BusinessActionType.MAILBOX_UPDATE_RC, message);

            String historyMessage = "Отправлена заявка на" + (state ? "включение" : "отключение") + "анти-спама у почтового ящика '"
                    + mailbox.getFullName() + "' в связи с " + (state ? "включением" : "отключением") + " услуги";
            history.save(accountId, historyMessage);
        }
    }

    public void processErrorActionForSwitchAccountResources(ProcessingBusinessAction action, ProcessingBusinessOperation operation, SimpleServiceMessage rabbitMessage, String resourceName) {
        String errorMessage = MapUtils.getString(rabbitMessage.getParams(), Constants.ERROR_MESSAGE_KEY, "");
        String resourceId = MapUtils.getString(action.getParams(), Constants.RESOURCE_ID_KEY);
        Boolean switchedOn = MapUtils.getBoolean(operation.getParams(), Constants.SWITCHED_ON_KEY, null);

        String switchStatus = switchedOn == null ? "null" : switchedOn ? "включении" : "выключении";
        String message = String.format("Ошибка при %s ресурса %s, id: %s. %s", switchStatus, resourceName, resourceId, errorMessage);
        // todo более адекватная обработка ошибок.
        businessHelper.addWarning(operation.getId(), message);
        history.save(action.getPersonalAccountId(), message);
        log.error("An error occurred while switching resources. accountId: {}, actionIdentity: {}, operationIdentity: {}, errorMessage: {}, switchedOn: {}, objRef: {}",
                operation.getPersonalAccountId(),
                rabbitMessage.getActionIdentity(),
                rabbitMessage.getOperationIdentity(),
                errorMessage,
                switchedOn,
                rabbitMessage.getObjRef()
        );
    }

    public ProcessingBusinessOperation switchAccountResourcesStartOperation(PersonalAccount account, boolean state) {
        SimpleServiceMessage messageBusinessOperation = new SimpleServiceMessage();
        messageBusinessOperation.setAccountId(account.getId());
        messageBusinessOperation.addParam(Constants.SWITCHED_ON_KEY, state);
        messageBusinessOperation.addParam(Constants.STAGE_KEY, SwitchAccountResourcesStage.CREATED_STAGE);
        ProcessingBusinessOperation operation = businessHelper.buildOperation(BusinessOperationType.SWITCH_ACCOUNT_RESOURCES, messageBusinessOperation);
        publisher.publishEvent(new SwitchAccountResourcesEvent(operation, state));
        return operation;
    }

    public void switchResourcesStartStageFirst(ProcessingBusinessOperation operation, boolean state) {
        log.info("Resource switching started. account Id: {}, operation id {}, state: {}", operation.getPersonalAccountId(), operation.getId(), state);
        try {
            businessHelper.setStage(operation.getId(), SwitchAccountResourcesStage.FIRST_PREPARE_STAGE);

            String accountId = operation.getPersonalAccountId();
            Assert.notNull(accountId, "The personalAccountId must be not null. Operation id: " + operation.getId());
            switchUnixAccounts(accountId, state, operation.getId()); // todo для тарифа у которого plan.isUnixAccountAllowed (Партнер), видимо тоже нужно не включать
            switchDomains(accountId, state, operation.getId());

            Assert.isTrue(businessHelper.setStage(operation.getId(), SwitchAccountResourcesStage.FIRST_SENT_ASYNC_STAGE, SwitchAccountResourcesStage.FIRST_PREPARE_STAGE), "Ошибка на стадии переключения unix-аккаунтов и доменов для операции: " + operation.getId());
            processEventsAmqpSwitchStartStageSecondIfNeed(operation);
        } catch (Exception e) {
            log.error(String.format("We got exception when switch account resources. AccountId: %s, operationId: %s", operation.getPersonalAccountId(), operation.getId()), e);
            businessHelper.setErrorStatus(operation.getId(), String.format("Ошибка при %s ресурсов аккаунта", state ? "включении" : "выключении"));
        }
    }

    public void processEventsAmqpSwitchStartStageSecondIfNeed(ProcessingBusinessOperation operation) {
        if (businessHelper.existsActiveActions(operation.getId())) {
            log.debug("Skip processEventsAmqpSwitchStartStageSecondIfNeed because there are active actions for operation: {}", operation.getId());
            return;
        }
        if (!businessHelper.setStage(operation.getId(), SwitchAccountResourcesStage.SECOND_PREPARE_ASYNC_STAGE, SwitchAccountResourcesStage.FIRST_SENT_ASYNC_STAGE)) {
            return;
        }
        Boolean state = null;
        try {
            Object stateObj = operation.getParam(Constants.SWITCHED_ON_KEY);
            Assert.isInstanceOf(Boolean.class, operation.getParam(Constants.SWITCHED_ON_KEY), String.format("Wrong parameter %s value: %s for ProcessingBusinessOperation: %s", Constants.SWITCHED_ON_KEY, stateObj, operation.getId()));
            state = (Boolean) stateObj;
            String accountId = operation.getPersonalAccountId();
            Assert.notNull(accountId, String.format("Wrong parameter personalAccountId value: %s for ProcessingBusinessOperation: %s", accountId, operation.getId()));

            PersonalAccount account = personalAccountManager.findOne(accountId);
            Plan plan = planManager.findOne(account.getPlanId());

            switchDedicatedAppServices(accountId, state, operation.getId());
            if (!state || plan.isDatabaseUserAllowed(accountServiceHelper.hasAllowUseDbService(accountId))) {
                switchDatabaseUsers(accountId, state, operation.getId());
            }
            switchFtpUsers(accountId, state, operation.getId());
            switchRedirects(accountId, state, operation.getId());
            switchMailboxes(accountId, state, operation.getId());
            switchWebsites(accountId, state, operation.getId());
            // todo не понятно почему не нужно включать/выключать базы данных и где они включаются на самом деле

            Assert.isTrue(businessHelper.setStage(operation.getId(), SwitchAccountResourcesStage.SECOND_SENT_ASYNC_STAGE), "Ошибка на стадии переключения менее важных ресурсов для операции: " + operation.getId());

            processEventsAmqpSwitchResourcesFinishIfNeed(operation);
        } catch (Exception e) {
            businessHelper.setErrorStatus(operation.getId(), String.format("Ошибка при %s ресурсов аккаунта", state == null ? null : state ? "включении" : "выключении"));
        }
    }

    public void processEventsAmqpSwitchResourcesFinishIfNeed(ProcessingBusinessOperation operation) {
        if (businessHelper.existsActiveActions(operation.getId())) {
            log.debug("Skip processEventsAmqpSwitchResourcesFinishIfNeed because there are active actions for operation: {}", operation.getId());
            return;
        }
        if (!businessHelper.setStage(operation.getId(), SwitchAccountResourcesStage.FINISH_STAGE, SwitchAccountResourcesStage.SECOND_SENT_ASYNC_STAGE)) {
            return;
        }
        businessHelper.setSuccessCompletedStatus(operation.getId(), null);
        log.info("Resource switching completed. account Id: {}, operation id {}, state: {}", operation.getPersonalAccountId(), operation.getId(), operation.getParam(Constants.SWITCHED_ON_KEY));
    }

    private void switchWebsites(PersonalAccount account, Boolean state) {
        switchWebsites(account.getId(), state, null);
    }

    private <TResource extends Resource> void switchResources(String accountId, boolean state, @Nullable String operationId, BusinessActionType businessActionType, Supplier<List<TResource>> getResources) {
        String resourceTypeForUser = Utils.humanizeResourceType(businessActionType.getResourceClass());
        try {
            AtomicInteger businessActionCount = new AtomicInteger();
            getResources.get().forEach(resource -> {
                SimpleServiceMessage message = messageForSwitchOn(resource, state, operationId);
                businessHelper.buildAction(businessActionType, message);
                if (operationId == null) {
                    String historyMessage = String.format("Отправлена заявка на %s ресурса: %s '%s'", (state ? "включение" : "выключение"), resourceTypeForUser, resource.getName());
                    history.save(accountId, historyMessage);
                }
                businessActionCount.getAndIncrement();
            });
            log.debug("Created {} businessAction type: {} for accountId {}, operationId: {}, state: {}", businessActionCount.get(), businessActionType, accountId, operationId, state);
        } catch (FeignException e) {
            log.error(String.format("Switch failed. Failed to get resources while send operations %s for accountId: %s", businessActionType, accountId), e);
            if (operationId != null) {
                businessHelper.addWarning(operationId, String.format("Не удалось загрузить ресурсы: %s",resourceTypeForUser));
            }
        } catch (Exception e) {
            log.error(String.format("Switch failed. Unknown exception while send operations %s for accountId: %s", businessActionType, accountId), e);
            if (operationId != null) {
                businessHelper.addWarning(operationId, String.format("Неизвестная ошибка во время включения ресурсов: %s" ,resourceTypeForUser));
            }
        }
    }

    private void switchWebsites(String accountId, boolean state, @Nullable String operationId) {
        switchResources(accountId, state, operationId, BusinessActionType.WEB_SITE_UPDATE_RC, () -> rcUserFeignClient.getWebSites(accountId));
    }

    /** todo must be private */
    public void switchDatabaseUsers(PersonalAccount account, Boolean state) {
        switchDatabaseUsers(account.getId(), state, null);
    }

    private void switchDatabaseUsers(String accountId, boolean state, @Nullable String operationId) {
        switchResources(accountId, state, operationId, BusinessActionType.DATABASE_USER_UPDATE_RC, () -> rcUserFeignClient.getDatabaseUsers(accountId));
    }

    public boolean haveDatabases(PersonalAccount account) {
        return !rcUserFeignClient.getDatabases(account.getId()).isEmpty();
    }

    public boolean haveDatabaseUsers(PersonalAccount account) {
        return !rcUserFeignClient.getDatabaseUsers(account.getId()).isEmpty();
    }

    private void switchDedicatedAppServices(String accountId, boolean state, @Nullable String operationId) {
        try {
            dedicatedAppServiceHelper.switchAllDedicatedAppService(accountId, state, operationId, (exception, rcStaffService) -> {
                if (StringUtils.isEmpty(operationId)) return;
                if (exception instanceof ResourceIsLockedException) {
                    businessHelper.addWarning(operationId, String.format("Выделенный сервис %s занят", rcStaffService.getId()));
                } else {
                    businessHelper.addWarning(operationId, String.format("Ошибка при обработке выделенного сервиса %s. %s", rcStaffService.getId(), exception.getMessage()));
                }
            });
        } catch (Exception e) {
            if (operationId != null) {
                businessHelper.addWarning(operationId, "Ошибка при переключении выделенных сервисов. " + e.getMessage());
            }
            log.error(String.format("account DedicatedAppServices switch failed for accountId: %s, state: %b, operationId: %s", accountId, state, operationId), e);
        }
    }

    /** todo must be private */
    public void switchDatabases(PersonalAccount account, Boolean state) {
        try {

            Collection<Database> databases = rcUserFeignClient.getDatabases(account.getId());

            for (Database database : databases) {
                if (state == null || state.equals(database.getSwitchedOn())) {
                    continue;
                }
                SimpleServiceMessage message = messageForSwitchOn(database, state, null);

                businessHelper.buildAction(BusinessActionType.DATABASE_UPDATE_RC, message);

                String historyMessage = "Отправлена заявка на " + (state ? "включение" : "выключение") + " пользователя базы данных '" + database.getName() + "'";
                history.save(account, historyMessage);
            }

        } catch (Exception e) {
            log.error("account Databases switch failed for accountId: " + account.getId());
            e.printStackTrace();
        }
    }

    private void switchMailboxes(PersonalAccount account, Boolean state) {
        switchMailboxes(account.getId(), state, null);
    }

    private void switchMailboxes(String accountId, boolean state, @Nullable String operationId) {
        switchResources(accountId, state, operationId, BusinessActionType.MAILBOX_UPDATE_RC, () -> rcUserFeignClient.getMailboxList(accountId));
    }

    public void switchDomains(PersonalAccount account, Boolean state) {
        switchDomains(account.getId(), state, null);
    }

    private void switchDomains(String accountId, boolean state, @Nullable String operationId) {
        switchResources(accountId, state, operationId, BusinessActionType.DOMAIN_UPDATE_RC, () -> rcUserFeignClient.getDomains(accountId));
    }

    private void switchFtpUsers(PersonalAccount account, Boolean state) {
        switchFtpUsers(account.getId(), state, null);
    }

    private void switchFtpUsers(String accountId, boolean state, @Nullable String operationId) {
        switchResources(accountId, state, operationId, BusinessActionType.FTP_USER_UPDATE_RC, () -> rcUserFeignClient.getFTPUsers(accountId));
    }

    private void switchUnixAccounts(PersonalAccount account, Boolean state) {
        switchUnixAccounts(account.getId(), state, null);
    }

    private void switchUnixAccounts(String accountId, boolean state, @Nullable String operationId) {
        switchResources(accountId, state, operationId, BusinessActionType.UNIX_ACCOUNT_UPDATE_RC, () -> rcUserFeignClient.getUnixAccountList(accountId));
    }

    private void switchRedirects(PersonalAccount account, Boolean state) {
        switchRedirects(account.getId(), state, null);
    }

    private void switchRedirects(String accountId, boolean state, @Nullable String operationId) {
        switchResources(accountId, state, operationId, BusinessActionType.REDIRECT_UPDATE_RC, () -> rcUserFeignClient.getRedirects(accountId));
    }

    private SimpleServiceMessage messageForSwitchOn(Resource resource, boolean state, @Nullable String operationId) {
        SimpleServiceMessage message = new SimpleServiceMessage();
        message.setParams(new HashMap<>());
        message.setAccountId(resource.getAccountId());
        message.addParam(Constants.RESOURCE_ID_KEY, resource.getId());
        message.addParam(Constants.SWITCHED_ON_KEY, state);
        message.setOperationIdentity(operationId);
        return message;
    }

    public void updateUnixAccountQuota(PersonalAccount account, Long quotaInBytes) {
        try {

            Collection<UnixAccount> unixAccounts = rcUserFeignClient.getUnixAccounts(account.getId());

            for (UnixAccount unixAccount : unixAccounts) {
                if (!unixAccount.getQuota().equals(quotaInBytes)) {
                    SimpleServiceMessage message = new SimpleServiceMessage();
                    message.setParams(new HashMap<>());
                    message.setAccountId(account.getId());
                    message.addParam("resourceId", unixAccount.getId());
                    message.addParam("quota", quotaInBytes);

                    businessHelper.buildAction(BusinessActionType.UNIX_ACCOUNT_UPDATE_RC, message);

                    String historyMessage = "Отправлена заявка на установку новой квоты в значение '" + quotaInBytes +
                            " байт' для UNIX-аккаунта '" + unixAccount.getName() + "'";
                    history.save(account, historyMessage);
                }
            }

        } catch (Exception e) {
            log.error("account UnixAccounts set quota failed for accountId: " + account.getId());
            e.printStackTrace();
        }
    }

    public void switchCertificates(PersonalAccount account, boolean state) {
        Collection<SSLCertificate> sslCertificates = rcUserFeignClient.getSSLCertificates(account.getId());

        for (SSLCertificate sslCertificate : sslCertificates) {
            SimpleServiceMessage message = new SimpleServiceMessage();
            message.setAccountId(account.getId());
            message.addParam("resourceId", sslCertificate.getId());
            message.addParam("switchedOn", state);

            businessHelper.buildAction(BusinessActionType.SSL_CERTIFICATE_UPDATE_RC, message);

            String historyMessage = "Отправлена заявка на выключение SSL сертификата '" + sslCertificate.getName() + "'";
            history.save(account, historyMessage);
        }
    }

    public void deleteRedirects(PersonalAccount account, String domainName) {
        rcUserFeignClient
                .getRedirects(account.getId())
                .stream()
                .filter(r -> r.getName().equals(domainName))
                .forEach(r -> {
                    SimpleServiceMessage message = new SimpleServiceMessage();
                    message.setParams(new HashMap<>());
                    message.setAccountId(account.getId());
                    message.addParam("resourceId", r.getId());

                    businessHelper.buildAction(BusinessActionType.REDIRECT_DELETE_RC, message);

                    String historyMessage = "Отправлена заявка на удаление переадресации '" + r.getName() + "'";
                    history.save(account, historyMessage);
                });
    }

    public void disableAndScheduleDeleteForAllMailboxes(PersonalAccount account) {
        Collection<Mailbox> mailboxes = rcUserFeignClient.getMailboxes(account.getId());

        for (Mailbox mailbox : mailboxes) {
            SimpleServiceMessage message = new SimpleServiceMessage();
            message.setParams(new HashMap<>());
            message.setAccountId(account.getId());
            message.addParam("resourceId", mailbox.getId());
            message.addParam("switchedOn", false);
            message.addParam(Constants.WILL_BE_DELETED_AFTER_KEY, LocalDateTime.now().plusDays(7).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            businessHelper.buildAction(BusinessActionType.MAILBOX_UPDATE_RC, message);

            String historyMessage = "Отправлена заявка на выключение и отложенное удаление почтового ящика '" + mailbox.getName() + "'";
            history.save(account, historyMessage);
        }
    }

    public void disableAndScheduleDeleteForAllDatabases(PersonalAccount account) {
        Collection<Database> databases = rcUserFeignClient.getDatabases(account.getId());

        for (Database database : databases) {
            SimpleServiceMessage message = new SimpleServiceMessage();
            message.setParams(new HashMap<>());
            message.setAccountId(account.getId());
            message.addParam("resourceId", database.getId());
            message.addParam("switchedOn", false);
            message.addParam(Constants.WILL_BE_DELETED_AFTER_KEY, LocalDateTime.now().plusDays(7).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            businessHelper.buildAction(BusinessActionType.DATABASE_UPDATE_RC, message);

            String historyMessage = "Отправлена заявка на выключение и отложенное удаление базы данных '" + database.getName() + "'";
            history.save(account, historyMessage);
        }
    }

    public void disableAndScheduleDeleteForAllDatabaseUsers(PersonalAccount account) {
        Collection<DatabaseUser> databaseUsers = rcUserFeignClient.getDatabaseUsers(account.getId());

        for (DatabaseUser databaseUser : databaseUsers) {
            SimpleServiceMessage message = new SimpleServiceMessage();
            message.setParams(new HashMap<>());
            message.setAccountId(account.getId());
            message.addParam("resourceId", databaseUser.getId());
            message.addParam("switchedOn", false);
            message.addParam(Constants.WILL_BE_DELETED_AFTER_KEY, LocalDateTime.now().plusDays(7).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            businessHelper.buildAction(BusinessActionType.DATABASE_USER_UPDATE_RC, message);

            String historyMessage = "Отправлена заявка на выключение и отложенное удаление пользователя баз данных '" + databaseUser.getName() + "'";
            history.save(account, historyMessage);
        }
    }

    public void unScheduleDeleteForAllMailboxes(PersonalAccount account) {
        Collection<Mailbox> mailboxes = rcUserFeignClient.getMailboxes(account.getId());

        for (Mailbox mailbox : mailboxes) {
            SimpleServiceMessage message = new SimpleServiceMessage();
            message.setParams(new HashMap<>());
            message.setAccountId(account.getId());
            message.addParam("resourceId", mailbox.getId());
            message.addParam(Constants.WILL_BE_DELETED_AFTER_KEY, null);

            businessHelper.buildAction(BusinessActionType.MAILBOX_UPDATE_RC, message);

            String historyMessage = "Отправлена заявка на отмену отложенного удаления почтового ящика '" + mailbox.getName() + "'";
            history.save(account, historyMessage);
        }
    }

    public boolean unScheduleDeleteForAllDatabases(PersonalAccount account) {
        Collection<Database> databases = rcUserFeignClient.getDatabases(account.getId());

        boolean isMarkDeletedDatabase = false;

        for (Database database : databases) {
            SimpleServiceMessage message = new SimpleServiceMessage();
            message.setParams(new HashMap<>());
            message.setAccountId(account.getId());
            message.addParam(Constants.RESOURCE_ID_KEY, database.getId());
            String historyMessage;
            if (database.isWillBeDeleted()) {
                isMarkDeletedDatabase = true;
                message.addParam(Constants.WILL_BE_DELETED_AFTER_KEY, null);
                message.addParam(Constants.SWITCHED_ON_KEY, true);
                historyMessage = "Отправлена заявка на отмену отложенного удаления базы данных '" + database.getName() + "'";
            } else {
                message.addParam(Constants.WILL_BE_DELETED_AFTER_KEY, null);
                message.addParam(Constants.SWITCHED_ON_KEY, true);
                historyMessage = "Отправлена заявка на включение базы данных '" + database.getName() + "'";
            }
            businessHelper.buildAction(BusinessActionType.DATABASE_UPDATE_RC, message);
            history.save(account, historyMessage);
        }
        return isMarkDeletedDatabase;
    }

    public boolean unScheduleDeleteForAllDatabaseUsers(PersonalAccount account) {
        Collection<DatabaseUser> databaseUsers = rcUserFeignClient.getDatabaseUsers(account.getId());

        boolean isMarkDeletedDatabase = false;

        for (DatabaseUser databaseUser : databaseUsers) {
            SimpleServiceMessage message = new SimpleServiceMessage();
            String historyMessage;
            message.setParams(new HashMap<>());
            message.setAccountId(account.getId());
            message.addParam(Constants.RESOURCE_ID_KEY, databaseUser.getId());

            if (databaseUser.isWillBeDeleted()) {
                message.addParam(Constants.SWITCHED_ON_KEY, true);
                message.addParam(Constants.WILL_BE_DELETED_AFTER_KEY, null);
                historyMessage = "Отправлена заявка на отмену отложенного удаления пользователя баз данных '" + databaseUser.getName() + "'";
            } else {
                message.addParam(Constants.WILL_BE_DELETED_AFTER_KEY, null);
                message.addParam(Constants.SWITCHED_ON_KEY, true);
                historyMessage = "Отправлена заявка на включение пользователя баз данных '" + databaseUser.getName() + "'";
            }

            businessHelper.buildAction(BusinessActionType.DATABASE_USER_UPDATE_RC, message);

            history.save(account, historyMessage);
        }
        return isMarkDeletedDatabase;
    }

    public List<Quotable> getQuotableResources(PersonalAccount account) {
        List<Quotable> quotableResources = new ArrayList<>();

        try {
            Collection<UnixAccount> unixAccounts = rcUserFeignClient.getUnixAccounts(account.getId());
            quotableResources.addAll(unixAccounts);
        } catch (Exception e) {
            log.error("get unixAccounts failed for accountId: " + account.getId());
            e.printStackTrace();
        }

        try {
            Collection<Mailbox> mailboxes = rcUserFeignClient.getMailboxes(account.getId());
            quotableResources.addAll(mailboxes);
        } catch (Exception e) {
            log.error("get Mailbox failed for accountId: " + account.getId());
            e.printStackTrace();
        }

        try {
            Collection<Database> databases = rcUserFeignClient.getDatabases(account.getId());
            quotableResources.addAll(databases);
        } catch (Exception e) {
            log.error("get Database failed for accountId: " + account.getId());
            e.printStackTrace();
        }
        return quotableResources;
    }

    public List<Quotable> filterQuotableResoursesByWritableState(List<Quotable> quotableResources, boolean state) {
        return quotableResources.stream().filter(
                quotableResource -> (quotableResource.getWritable() == state)
        ).collect(Collectors.toList());
    }

    public void setWritableForAccountQuotaServicesByList(PersonalAccount account, Boolean state, List<Quotable> resourses) {

        for (Quotable resource: resourses) {
            try {
                if (resource instanceof UnixAccount) {

                    setWritableForUnixAccount(account, (UnixAccount) resource, state);

                } else if (resource instanceof Mailbox) {

                    setWritableForMailbox(account, (Mailbox) resource, state);

                } else if (resource instanceof Database) {

                    setWritableForDatabase(account, (Database) resource, state);

                } else {

                    log.error("can't cast resource [" + resource + "] for accountId: " + account.getId());
                }
            } catch (Exception e) {
                log.error("account resource [" + resource + "] writable switch failed for accountId: " + account.getId());
                e.printStackTrace();
            }
        }
    }

    private void setWritableForUnixAccount(PersonalAccount account, UnixAccount unixAccount, boolean state) {

        try {
            SimpleServiceMessage message = new SimpleServiceMessage();
            message.setParams(new HashMap<>());
            message.setAccountId(account.getId());
            message.addParam("resourceId", unixAccount.getId());
            message.addParam("writable", state);

            businessHelper.buildAction(BusinessActionType.UNIX_ACCOUNT_UPDATE_RC, message);

            String historyMessage = "Отправлена заявка на " + (state ? "включение" : "выключение") +
                    " возможности записывать данные (writable) для UNIX-аккаунта '" + unixAccount.getName() + "'";
            history.save(account, historyMessage);

        } catch (Exception e) {
            log.error("account unixAccount [" + unixAccount.getId() + "] writable switch failed for accountId: " + account.getId());
            e.printStackTrace();
        }
    }

    private void setWritableForMailbox(PersonalAccount account, Mailbox mailbox, boolean state) {

        try {
            SimpleServiceMessage message = new SimpleServiceMessage();
            message.setParams(new HashMap<>());
            message.setAccountId(account.getId());
            message.addParam("resourceId", mailbox.getId());
            message.addParam("writable", state);

            businessHelper.buildAction(BusinessActionType.MAILBOX_UPDATE_RC, message);

            String historyMessage = "Отправлена заявка на " + (state ? "включение" : "выключение") +
                    " возможности сохранять письма (writable) для почтового ящика '" + mailbox.getFullName() + "'";
            history.save(account, historyMessage);


        } catch (Exception e) {
            log.error("account Mailbox [" + mailbox.getId() + "] writable switch failed for accountId: " + account.getId());
            e.printStackTrace();
        }
    }

    private void setWritableForDatabase(PersonalAccount account, Database database, Boolean state) {

        try {
            SimpleServiceMessage message = new SimpleServiceMessage();
            message.setParams(new HashMap<>());
            message.setAccountId(account.getId());
            message.addParam("resourceId", database.getId());
            message.addParam("writable", state);

            businessHelper.buildAction(BusinessActionType.DATABASE_UPDATE_RC, message);

            String historyMessage = "Отправлена заявка на " + (state ? "включение" : "выключение") +
                    " возможности записывать данные (writable) для базы данных '" + database.getName() + "'";
            history.save(account, historyMessage);

        } catch (Exception e) {
            log.error("account Database [" + database.getName() + "] writable switch failed for accountId: " + account.getId());
            e.printStackTrace();
        }
    }
}
