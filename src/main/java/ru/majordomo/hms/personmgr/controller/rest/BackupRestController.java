package ru.majordomo.hms.personmgr.controller.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestWrapper;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.majordomo.hms.personmgr.common.BusinessOperationType;
import ru.majordomo.hms.personmgr.common.StorageType;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.dto.request.FileRestoreRequest;
import ru.majordomo.hms.personmgr.dto.request.MysqlRestoreRequest;
import ru.majordomo.hms.personmgr.dto.request.RestoreRequest;
import ru.majordomo.hms.personmgr.exception.InternalApiException;
import ru.majordomo.hms.personmgr.service.restic.Snapshot;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessAction;
import ru.majordomo.hms.personmgr.service.RcStaffFeignClient;
import ru.majordomo.hms.personmgr.service.RcUserFeignClient;
import ru.majordomo.hms.personmgr.service.restic.ResticClient;
import ru.majordomo.hms.personmgr.validation.ObjectId;
import ru.majordomo.hms.rc.staff.resources.Server;
import ru.majordomo.hms.rc.user.resources.Database;
import ru.majordomo.hms.rc.user.resources.UnixAccount;

import javax.validation.Valid;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static ru.majordomo.hms.personmgr.common.BusinessActionType.DATABASE_RESTORE_TE;
import static ru.majordomo.hms.personmgr.common.BusinessActionType.FILE_BACKUP_RESTORE_TE;
import static ru.majordomo.hms.personmgr.common.Constants.*;
import static ru.majordomo.hms.personmgr.common.Utils.isInsideInRootDir;

@RestController
@Validated
public class BackupRestController extends CommonRestController{

    private RcUserFeignClient rcUserFeignClient;
    private RcStaffFeignClient rcStaffFeignClient;
    private ResticClient resticClient;

    private String mysqlBackupStorage;
    private final static String RC_USER_APP_NAME = "rc-user";
    private final static String UNIX_ACCOUNT_RESOURCE_NAME = "unix-account";
    private final static String DATABASE_RESOURCE_NAME = "database";

    @Autowired
    public BackupRestController(
            RcUserFeignClient rcUserFeignClient,
            RcStaffFeignClient rcStaffFeignClient,
            ResticClient resticClient,
            @Value("${backup.mysql.url}") String mysqlBackupStorage
    ) {
        this.rcUserFeignClient = rcUserFeignClient;
        this.rcStaffFeignClient = rcStaffFeignClient;
        this.resticClient = resticClient;
        this.mysqlBackupStorage = mysqlBackupStorage;
    }

    @GetMapping("/{accountId}/backup")
    public List<Snapshot> getSnapshots(
            @ObjectId(PersonalAccount.class) @PathVariable String accountId,
            @RequestParam StorageType type,
            Authentication authentication
    ) {
        switch (type) {
            case FILE:
                return getFileSnapshots(accountId, authentication);
            case MYSQL:
                return getDbSnapshots(accountId, authentication);
            default:
                throw new ParameterValidationException("Неизвестный тип восстановления");
        }
    }

    @GetMapping("/{accountId}/file-backup")
    public List<Snapshot> getSnapshot(
            @ObjectId(PersonalAccount.class) @PathVariable String accountId,
            Authentication authentication
    ) {
        return getFileSnapshots(accountId, authentication);
    }

    @PostMapping("/{accountId}/backup/restore")
    public ResponseEntity<SimpleServiceMessage> restore(
            @Valid @RequestBody RestoreRequest restoreRequest,
            @ObjectId(PersonalAccount.class) @PathVariable String accountId,
            SecurityContextHolderAwareRequestWrapper request
    ) {
        logger.info("Try restore from backup accountId: %s %s", accountId, restoreRequest.toString());

        SimpleServiceMessage result;

        if (restoreRequest instanceof MysqlRestoreRequest) {
            result = restoreMysql(accountId, (MysqlRestoreRequest) restoreRequest, request);
        } else if (restoreRequest instanceof FileRestoreRequest) {
            result = restoreFileBackup(accountId, (FileRestoreRequest) restoreRequest, request);
        } else {
            throw new ParameterValidationException("Неизвестный тип восстановления");
        }

        return ResponseEntity.accepted().body(result);
    }

    private SimpleServiceMessage restoreMysql(
            String accountId,
            MysqlRestoreRequest restoreRequest,
            SecurityContextHolderAwareRequestWrapper request
    ) {
        PersonalAccount account = accountManager.findOne(accountId);
        assertAccountIsActive(account);

        int daysAccessToBackup = daysAccessToBackup(account);
        if (daysAccessToBackup == 0) {
            throw new ParameterValidationException("Восстановление из резервных копий недоступно");
        }

        Database database = rcUserFeignClient.getDatabase(accountId, restoreRequest.getDatabaseId());
        checkDatabase(database);

        Optional<Snapshot> snapshotOptional = resticClient.getDBSnapshot(
                account.getId(), restoreRequest.getSnapshotId());

        if (!snapshotOptional.isPresent()) {
            throw new ParameterValidationException(
                    "Резервная копия с id " + restoreRequest.getSnapshotId() + " не найдена");
        }

        Snapshot snapshot = snapshotOptional.get();
        if (snapshot.getTime().toLocalDate().isBefore(LocalDate.now().minusDays(daysAccessToBackup))) {
            throw new ParameterValidationException("Восстановление из резервной копии недоступно");
        }

        if (snapshot.getPaths().size() != 1) {
            logger.error("Имя файла дампа базы данных не найдено в snapshot.getPaths() snapshot: %s", snapshot);
            throw new InternalApiException();
        }

        Server server = getServerByServiceId(database.getServiceId());

        SimpleServiceMessage message = new SimpleServiceMessage();
        message.setAccountId(accountId);
        message.addParam("realRoutingKey", getRoutingKeyForTE(server));
        message.setObjRef(format("http://%s/%s/%s", RC_USER_APP_NAME, DATABASE_RESOURCE_NAME, database.getId()));
        message.addParam(DATASOURCE_URI_KEY, format(
                "%s/mysql/ids/%s/%s", mysqlBackupStorage, snapshot.getShortId(), snapshot.getPaths().get(0))
        );

        Map<String, Object> dataSourceParams = new HashMap<>();
        dataSourceParams.put(DELETE_EXTRANEOUS_KEY, restoreRequest.getDeleteExtraneous());
        message.addParam(DATA_SOURCE_PARAMS_KEY, dataSourceParams);

        ProcessingBusinessAction action = businessHelper.buildActionAndOperation(
                BusinessOperationType.DATABASE_BACKUP_RESTORE, DATABASE_RESTORE_TE, message);

        history.save(
                accountId,
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

    private void checkDatabase(Database database) {
        if (!database.isSwitchedOn()) {
            throw new ParameterValidationException("База данных отключена");
        }
        if (!database.getWritable()) {
            throw new ParameterValidationException("Запись в базу данных запрещена");
        }
    }

    private List<Snapshot> getDbSnapshots(String personalAccountId, Authentication authentication) {
        PersonalAccount account = accountManager.findOne(personalAccountId);

        int daysAccessToBackup = daysAccessToBackup(account);
        if (daysAccessToBackup == 0) {
            return Collections.emptyList();
        }

        List<Snapshot> response = resticClient.getDBSnapshots(account.getId());

        LocalDate minTimeForBackup = LocalDate.now().minusDays(daysAccessToBackup);

        return response
                .stream()
                .filter(i ->
                        i.getTime().toLocalDate()
                                .isAfter(minTimeForBackup))
                .collect(Collectors.toList());
    }

    private List<Snapshot> getFileSnapshots(String personalAccountId, Authentication authentication) {
        PersonalAccount account = accountManager.findOne(personalAccountId);

        int daysAccessToBackup = daysAccessToBackup(account);
        if (daysAccessToBackup == 0) {
            return Collections.emptyList();
        }

        UnixAccount unixAccount = getUnixAccount(personalAccountId);
        Server server = getServer(unixAccount.getServerId());

        List<Snapshot> response = resticClient.getFileSnapshots(unixAccount.getHomeDir(), server.getName());

        LocalDate minTimeForBackup = LocalDate.now().minusDays(daysAccessToBackup);

        return response
                .stream()
                .filter(i ->
                        i.getTime().toLocalDate()
                                .isAfter(minTimeForBackup))
                .collect(Collectors.toList());
    }

    @PostMapping("/{accountId}/file-backup/restore")
    public ResponseEntity<SimpleServiceMessage> restoreFiles(
            @Valid @RequestBody FileRestoreRequest restoreRequest,
            @ObjectId(PersonalAccount.class) @PathVariable String accountId,
            SecurityContextHolderAwareRequestWrapper request
    ) {
        SimpleServiceMessage body = restoreFileBackup(accountId, restoreRequest, request);

        return ResponseEntity.accepted().body(body);
    }

    private SimpleServiceMessage restoreFileBackup(
            String accountId,
            FileRestoreRequest restoreRequest,
            SecurityContextHolderAwareRequestWrapper request
    ) {
        logger.debug("Try restore file from backup accountId: " + accountId + restoreRequest.toString());

        String serverName = restoreRequest.getServerName();
        String snapshotId = restoreRequest.getSnapshotId();
        String pathFrom = restoreRequest.getPathFrom();
        String pathTo = restoreRequest.getPathTo();
        Boolean deleteExtraneous = restoreRequest.getDeleteExtraneous();

        PersonalAccount account = accountManager.findOne(accountId);
        assertAccountIsActive(account);

        int daysAccessToBackup = daysAccessToBackup(account);
        if (daysAccessToBackup == 0) {
            throw new ParameterValidationException("Восстановление из резервных копий недоступно");
        }

        UnixAccount unixAccount = getUnixAccount(accountId);
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

//        checkPaths(pathFrom, pathTo, homeDir);

        Server server = getServer(unixAccount.getServerId());
        serverName = serverName == null || serverName.isEmpty() ? server.getName() : serverName;

        Optional<Snapshot> snapshotOptional = resticClient.getFileSnapshot(unixAccount.getHomeDir(), serverName, snapshotId);

        if (!snapshotOptional.isPresent()) {
            throw new ParameterValidationException("Резервная копия не найдена");
        }

        Snapshot snapshot = snapshotOptional.get();

        if (snapshot.getTime().toLocalDate().isBefore(LocalDate.now().minusDays(daysAccessToBackup))) {
            throw new ParameterValidationException("Восстановление из резервной копии недоступно");
        }

        SimpleServiceMessage message = new SimpleServiceMessage();
        message.setAccountId(accountId);
        message.addParam("realRoutingKey", getRoutingKeyForTE(server));
        message.setObjRef(format("http://%s/%s/%s", RC_USER_APP_NAME, UNIX_ACCOUNT_RESOURCE_NAME, unixAccount.getId()));
        message.addParam(DATA_DESTINATION_URI_KEY, format("file://%s", pathTo));
        message.addParam(DATASOURCE_URI_KEY, format("rsync://restic@bareos.intr/restic/%s/ids/%s%s", serverName, snapshotId, pathFrom));

        Map<String, Object> dataSourceParams = new HashMap<>();
        dataSourceParams.put(DELETE_EXTRANEOUS_KEY, deleteExtraneous);
        message.addParam(DATA_SOURCE_PARAMS_KEY, dataSourceParams);

        ProcessingBusinessAction action = businessHelper.buildActionAndOperation(
                BusinessOperationType.FILE_BACKUP_RESTORE, FILE_BACKUP_RESTORE_TE, message);

        history.save(
                accountId,
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

    private void checkPaths(String pathFrom, String pathTo, String homeDir) throws ParameterValidationException {
        if (pathFrom == null || pathFrom.isEmpty() || !isInsideInRootDir(pathFrom, homeDir)) {
            String errorMessage = "Параметр 'pathFrom' должен находиться внутри домашней директории unixAccount'а " + homeDir;
            throw new ParameterValidationException(errorMessage);
        }

        if (pathTo == null || pathTo.isEmpty() || !isInsideInRootDir(pathTo, homeDir)) {
            String errorMessage = "Параметр 'pathTo' должен находиться внутри домашней директории unixAccount'а " + homeDir;
            throw new ParameterValidationException(errorMessage);
        }
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

    private int daysAccessToBackup(PersonalAccount account) {
        if (!account.isActive()) {
            return 0;
        }
        return 7;
    }

    private void assertAccountIsActive(PersonalAccount account) {
        if (!account.isActive()) {
            throw new ParameterValidationException("Аккаунт не активен");
        }
    }
}
