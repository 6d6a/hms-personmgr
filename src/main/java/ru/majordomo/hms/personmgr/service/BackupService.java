package ru.majordomo.hms.personmgr.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestWrapper;
import org.springframework.stereotype.Service;
import ru.majordomo.hms.personmgr.common.BusinessOperationType;
import ru.majordomo.hms.personmgr.common.StorageType;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.dto.request.FileRestoreRequest;
import ru.majordomo.hms.personmgr.dto.request.MysqlRestoreRequest;
import ru.majordomo.hms.personmgr.exception.InternalApiException;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.manager.AccountHistoryManager;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessAction;
import ru.majordomo.hms.personmgr.service.restic.ResticClient;
import ru.majordomo.hms.personmgr.service.restic.Snapshot;
import ru.majordomo.hms.rc.staff.resources.Server;
import ru.majordomo.hms.rc.user.resources.Database;
import ru.majordomo.hms.rc.user.resources.UnixAccount;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static ru.majordomo.hms.personmgr.common.BusinessActionType.DATABASE_RESTORE_TE;
import static ru.majordomo.hms.personmgr.common.BusinessActionType.FILE_BACKUP_RESTORE_TE;
import static ru.majordomo.hms.personmgr.common.Constants.*;
import static ru.majordomo.hms.personmgr.common.Utils.isInsideInRootDir;

@Service
public class BackupService {

    private Logger logger = LoggerFactory.getLogger(BackupService.class);

    private RcUserFeignClient rcUserFeignClient;
    private RcStaffFeignClient rcStaffFeignClient;
    private ResticClient resticClient;
    private BusinessHelper businessHelper;
    private AccountHistoryManager history;

    private String mysqlBackupStorage;
    private final static String RC_USER_APP_NAME = "rc-user";
    private final static String UNIX_ACCOUNT_RESOURCE_NAME = "unix-account";
    private final static String DATABASE_RESOURCE_NAME = "database";

    @Autowired
    public BackupService(
            RcUserFeignClient rcUserFeignClient,
            RcStaffFeignClient rcStaffFeignClient,
            ResticClient resticClient,
            BusinessHelper businessHelper, AccountHistoryManager history, @Value("${backup.mysql.url}") String mysqlBackupStorage
    ) {
        this.rcUserFeignClient = rcUserFeignClient;
        this.rcStaffFeignClient = rcStaffFeignClient;
        this.resticClient = resticClient;
        this.businessHelper = businessHelper;
        this.history = history;
        this.mysqlBackupStorage = mysqlBackupStorage;
    }

    public SimpleServiceMessage restoreMysql(
            PersonalAccount account,
            MysqlRestoreRequest restoreRequest,
            SecurityContextHolderAwareRequestWrapper request
    ) {
        Database database = rcUserFeignClient.getDatabase(account.getId(), restoreRequest.getDatabaseId());
        checkDatabase(database);

        Optional<Snapshot> snapshotOptional = resticClient.getDBSnapshot(
                account.getId(), restoreRequest.getSnapshotId());

        if (!snapshotOptional.isPresent()) {
            throw new ParameterValidationException(
                    "Резервная копия с id " + restoreRequest.getSnapshotId() + " не найдена");
        }

        Snapshot snapshot = snapshotOptional.get();

        LocalDate minDateForBackup = minDateForBackup(account, StorageType.MYSQL);

        if (snapshot.getTime().toLocalDate().isBefore(minDateForBackup)) {
            throw new ParameterValidationException("Восстановление из резервной копии недоступно");
        }

        if (snapshot.getPaths().size() != 1) {
            logger.error("Имя файла дампа базы данных не найдено в snapshot.getPaths() snapshot: %s", snapshot);
            throw new InternalApiException();
        }

        SimpleServiceMessage message = messageForRestore(database, snapshot, restoreRequest.getDeleteExtraneous());

        ProcessingBusinessAction action = businessHelper.buildActionAndOperation(
                BusinessOperationType.DATABASE_BACKUP_RESTORE, DATABASE_RESTORE_TE, message);

        history.save(
                account.getId(),
                format("Поступила заявка на восстановление базы данных (databaseId='%s') из резервной копии " +
                                "(snapshotId: %s, time: %s) %s",
                        database.getId(), snapshot.getShortId(), snapshot.getTime(), restoreRequest.toString()
                ),
                request
        );

        SimpleServiceMessage result = new SimpleServiceMessage();
        result.setOperationIdentity(action.getOperationId());
        result.setActionIdentity(action.getId());
        return result;
    }

    public void restoreDbSilence(Database database, Snapshot snapshot, Boolean deleteExtraneous) {
        SimpleServiceMessage message = messageForRestore(database, snapshot, deleteExtraneous);
        businessHelper.buildAction(DATABASE_RESTORE_TE, message);
    }

    private SimpleServiceMessage messageForRestore(Database database, Snapshot snapshot, Boolean deleteExtraneous) {
        Server server = getServerByServiceId(database.getServiceId());

        SimpleServiceMessage message = new SimpleServiceMessage();
        message.setAccountId(database.getAccountId());
        message.addParam("realRoutingKey", getRoutingKeyForTE(server));
        message.setObjRef(format("http://%s/%s/%s", RC_USER_APP_NAME, DATABASE_RESOURCE_NAME, database.getId()));
        message.addParam(DATASOURCE_URI_KEY, format(
                "%s/mysql/ids/%s/%s", mysqlBackupStorage, snapshot.getShortId(), snapshot.getPaths().get(0))
        );

        Map<String, Object> dataSourceParams = new HashMap<>();
        dataSourceParams.put(DELETE_EXTRANEOUS_KEY, deleteExtraneous);
        message.addParam(DATA_SOURCE_PARAMS_KEY, dataSourceParams);
        return message;
    }

    private void checkDatabase(Database database) {
        if (!database.isSwitchedOn()) {
            throw new ParameterValidationException("База данных отключена");
        }
        if (!database.getWritable()) {
            throw new ParameterValidationException("Запись в базу данных запрещена");
        }
    }

    public List<Snapshot> getDbSnapshots(PersonalAccount account) {

        return resticClient.getDBSnapshots(account.getId());
    }

    public List<Snapshot> getFileSnapshots(PersonalAccount account) {
        UnixAccount unixAccount = getUnixAccount(account.getId());
        Server server = getServer(unixAccount.getServerId());

        return resticClient.getFileSnapshots(unixAccount.getHomeDir(), server.getName());
    }

    public SimpleServiceMessage restoreFileBackup(
            PersonalAccount account,
            FileRestoreRequest restoreRequest,
            SecurityContextHolderAwareRequestWrapper request
    ) {
        logger.debug("Try restore file from backup accountId: " + account.getId() + restoreRequest.toString());

        String serverName = restoreRequest.getServerName();
        String snapshotId = restoreRequest.getSnapshotId();
        String pathFrom = restoreRequest.getPathFrom();
        String pathTo = restoreRequest.getPathTo();
        Boolean deleteExtraneous = restoreRequest.getDeleteExtraneous();

        UnixAccount unixAccount = getUnixAccount(account.getId());
        checkUnixAccount(unixAccount);

        String homeDir = normalizeHomeDir(unixAccount.getHomeDir());

        pathFrom = normalizePathFrom(homeDir + "/" + pathFrom);

        assertPathFromInsideHomeDir(pathFrom, homeDir);

        if (pathTo == null) {
            pathTo = createPathTo(pathFrom);
        } else if (pathTo.isEmpty()) {
            pathTo = normalizePathTo(pathFrom, homeDir);
        } else {
            pathTo = normalizePathTo(pathFrom, homeDir + "/" + pathTo.replaceAll("^/+", ""));
        }

        Server server = getServer(unixAccount.getServerId());
        serverName = serverName == null || serverName.isEmpty() ? server.getName() : serverName;

        Optional<Snapshot> snapshotOptional = resticClient.getFileSnapshot(unixAccount.getHomeDir(), serverName, snapshotId);

        if (!snapshotOptional.isPresent()) {
            throw new ParameterValidationException("Резервная копия не найдена");
        }

        Snapshot snapshot = snapshotOptional.get();

        LocalDate minDateForBackup = minDateForBackup(account, StorageType.FILE);

        if (snapshot.getTime().toLocalDate().isBefore(minDateForBackup)) {
            throw new ParameterValidationException("Восстановление из резервной копии недоступно");
        }

        SimpleServiceMessage message = messageForRestore(
                unixAccount, server, pathTo, pathFrom, deleteExtraneous, serverName, snapshotId);

        ProcessingBusinessAction action = businessHelper.buildActionAndOperation(
                BusinessOperationType.FILE_BACKUP_RESTORE, FILE_BACKUP_RESTORE_TE, message);

        history.save(
                account,
                format("Поступила заявка на восстановление из резервной копии (snapshotId: %s, time: %s) %s",
                        snapshotId, snapshot.getTime(), restoreRequest.toString()
                ),
                request
        );

        SimpleServiceMessage result = new SimpleServiceMessage();
        result.setActionIdentity(action.getId());
        result.setOperationIdentity(action.getOperationId());
        return result;
    }

    public void restoreAllFiles(UnixAccount unixAccount, Snapshot snapshot) {
        SimpleServiceMessage message = messageForRestore(unixAccount, snapshot);
        businessHelper.buildAction(FILE_BACKUP_RESTORE_TE, message);
    }

    private SimpleServiceMessage messageForRestore(
            UnixAccount unixAccount,
            Snapshot snapshot
    ) {
        Server server = getServer(unixAccount.getServerId());
        String pathFrom = normalizeHomeDir(unixAccount.getHomeDir());
        String pathTo = createPathTo(pathFrom);

        if (!pathTo.startsWith("/home")) {
            throw new ParameterValidationException("pathTo not start with '/home' " + unixAccount.toString() + " " + snapshot.toString());
        }

        return messageForRestore(
                unixAccount,
                server,
                pathTo,
                pathFrom,
                true,
                snapshot.getServerName(),
                snapshot.getShortId()
        );
    }

    private SimpleServiceMessage messageForRestore(
            UnixAccount unixAccount,
            Server server,
            String pathTo,
            String pathFrom,
            Boolean deleteExtraneous,
            String serverName,
            String snapshotId
    ) {
        SimpleServiceMessage message = new SimpleServiceMessage();
        message.setAccountId(unixAccount.getAccountId());
        message.addParam("realRoutingKey", getRoutingKeyForTE(server));
        message.setObjRef(format("http://%s/%s/%s", RC_USER_APP_NAME, UNIX_ACCOUNT_RESOURCE_NAME, unixAccount.getId()));
        message.addParam(DATA_DESTINATION_URI_KEY, format("file://%s", pathTo));
        message.addParam(DATASOURCE_URI_KEY, format("rsync://restic@bareos.intr/restic/%s/ids/%s%s", serverName, snapshotId, pathFrom));

        Map<String, Object> dataSourceParams = new HashMap<>();
        dataSourceParams.put(DELETE_EXTRANEOUS_KEY, deleteExtraneous);
        message.addParam(DATA_SOURCE_PARAMS_KEY, dataSourceParams);
        return message;
    }

    private String createPathTo(String pathFrom) {
        List<String> locationsFrom = Arrays.asList(pathFrom.split("/"));
        return String.join("/", locationsFrom.subList(0, locationsFrom.size() - 1)) + "/";
    }

    private String normalizePathTo(String pathFrom, String pathTo) {
        List<String> locationsFrom = Arrays.asList(pathFrom.split("/"));
        String lastLocation = locationsFrom.get(locationsFrom.size() - 1);
        if (pathTo.endsWith(lastLocation)) {
            return pathTo.substring(0, pathTo.length() - lastLocation.length());
        } else {
            return pathTo;
        }
    }

    private void assertPathFromInsideHomeDir(String pathFrom, String homeDir) {
        if(!isInsideInRootDir(pathFrom, homeDir)) {
            throw new ParameterValidationException(
                    "Параметр 'pathFrom' должен находиться внутри домашней директории unixAccount'а " + homeDir);
        }
    }

    private String normalizePathFrom(String sourcePathFrom) {
        return sourcePathFrom.replaceAll("/+$", "");
    }

    private String normalizeHomeDir(String sourceHomeDir) {
        return sourceHomeDir.replaceAll("/+$", "");
    }

    private String getRoutingKeyForTE(Server server) {
        return "te." + server.getName().split("\\.")[0];
    }

    private Server getServer(String serverId) throws ParameterValidationException {
        try {
            return rcStaffFeignClient.getServerById(serverId);
        } catch (Exception ignore) {
            logger.error(format("Не найден сервер с id %s class: %s message: %s",
                    serverId, ignore.getClass().getName(), ignore.getMessage())
            );
            throw new ParameterValidationException("Сервер не найден");
        }
    }

    private Server getServerByServiceId(String serviceId) throws ParameterValidationException {
        try {
            return rcStaffFeignClient.getServerByServiceId(serviceId);
        } catch (Exception ignore) {
            logger.error(format("Не найден сервер с serviceId %s class: %s message: %s",
                    serviceId, ignore.getClass().getName(), ignore.getMessage())
            );
            throw new ParameterValidationException("Сервер не найден");
        }
    }

    private void checkUnixAccount(UnixAccount unixAccount) throws ParameterValidationException {
        if (!unixAccount.isSwitchedOn()) {
            String errorMessage = "UnixAccount выключен";
            throw new ParameterValidationException(errorMessage);
        }

        if (!unixAccount.getWritable()) {
            String errorMessage = "У UnixAccount'а отключена возможность записи данных";
            throw new ParameterValidationException(errorMessage);
        }
    }

    private UnixAccount getUnixAccount(String accountId) {
        Collection<UnixAccount> unixAccounts = rcUserFeignClient.getUnixAccounts(accountId);

        if (unixAccounts == null || unixAccounts.isEmpty()) {
            throw new ParameterValidationException("Не найден UnixAccount");
        }

        return unixAccounts.iterator().next();
    }

    public int daysAccessToBackup(PersonalAccount account, StorageType type) {
        return 7;
    }

    public LocalDate minDateForBackup(PersonalAccount account, StorageType type){
        return LocalDate.now().minusDays(daysAccessToBackup(account, type));
    }

    public void restoreAccountAfterEnabled(PersonalAccount account, LocalDateTime deactivated, LocalDate dataWillBeDeletedAfter) {

        restoreAllDb(account, deactivated, dataWillBeDeletedAfter);

        restoreUnixAccount(account, deactivated, dataWillBeDeletedAfter);
    }

    private void restoreAllDb(PersonalAccount account, LocalDateTime deactivated, LocalDate dataWillBeDeletedAfter) {
        List<Snapshot> allDbSnapshots = getDbSnapshots(account);

        List<Snapshot> inactiveTimeDbSnapshots = getSnapshotsByTimeBetween(
                allDbSnapshots, dataWillBeDeletedAfter, deactivated.toLocalDate());

        if (inactiveTimeDbSnapshots.isEmpty()) {
            history.save(
                    account,
                    "Не найдено резервных копий баз данных для восстановления после включения, deactivated: "
                            + deactivated.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            );
            return;
        }

        Collection<Database> databases = rcUserFeignClient.getDatabases(account.getId());
        for (Database database: databases) {
            Optional<Snapshot> snapshotOptional = inactiveTimeDbSnapshots
                    .stream()
                    .filter(snapshot ->
                            snapshot.getPaths().get(0).replaceAll("(\\.sql|\\.gz)$", "")
                                    .equals(database.getName())
                    ).findFirst();

            if (snapshotOptional.isPresent()) {
                Snapshot snapshot = snapshotOptional.get();
                restoreDbSilence(database, snapshot, true);
                history.save(account, "Отправлена заявка на восстановление базы '" + database.getName() + "' за дату "
                        + snapshot.getTime());
            } else {
                history.save(account, "Не найдено резервных копий базы данных '" + database.getName()
                        + "' для восстановления после включения, deactivated: "
                        + deactivated.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                );
            }
        }
    }

    private void restoreUnixAccount(PersonalAccount account, LocalDateTime deactivated, LocalDate dataWillBeDeletedAfter) {
        List<Snapshot> allFileSnapshots = getFileSnapshots(account);

        List<Snapshot> inactiveTimeFileSnapshots = getSnapshotsByTimeBetween(
                allFileSnapshots, dataWillBeDeletedAfter, deactivated.toLocalDate());

        if (inactiveTimeFileSnapshots.isEmpty()) {
            history.save(account, "Не найдено резервных копий unixAccount'а для восстановления после включения, deactivated: "
                    + deactivated.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            );
            return;
        }

        Collection<UnixAccount> unixAccounts = rcUserFeignClient.getUnixAccounts(account.getId());
        for (UnixAccount unixAccount: unixAccounts) {
            Snapshot snapshot = inactiveTimeFileSnapshots.get(0);
            restoreAllFiles(unixAccount, snapshot);
        }
    }

    private List<Snapshot> getSnapshotsByTimeBetween(List<Snapshot> snapshots, LocalDate after, LocalDate before) {
        return snapshots
                .stream()
                .filter(snapshot ->
                        snapshot.getTime().toLocalDate().isBefore(after)
                                &&
                                snapshot.getTime().toLocalDate().isAfter(before)
                )
                .sorted(Comparator.comparing(Snapshot::getTime))
                .collect(Collectors.toList());
    }
}