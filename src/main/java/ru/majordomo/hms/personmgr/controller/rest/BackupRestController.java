package ru.majordomo.hms.personmgr.controller.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestWrapper;
import org.springframework.web.bind.annotation.*;
import ru.majordomo.hms.personmgr.common.BusinessOperationType;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.exception.ResourceNotFoundException;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessAction;
import ru.majordomo.hms.personmgr.service.RcStaffFeignClient;
import ru.majordomo.hms.personmgr.service.RcUserFeignClient;
import ru.majordomo.hms.personmgr.validation.ObjectId;
import ru.majordomo.hms.rc.staff.resources.Server;
import ru.majordomo.hms.rc.user.resources.UnixAccount;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static java.lang.String.format;
import static ru.majordomo.hms.personmgr.common.BusinessActionType.FILE_BACKUP_RESTORE_TE;
import static ru.majordomo.hms.personmgr.common.Constants.DATADESTINATION_URI_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.DATASOURCE_URI_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.TE_PARAMS_KEY;
import static ru.majordomo.hms.personmgr.common.Utils.isInsideInRootDir;

@RestController
public class BackupRestController extends CommonRestController{

    private RcUserFeignClient rcUserFeignClient;
    private RcStaffFeignClient rcStaffFeignClient;

    private final static String RC_USER_APP_NAME = "rc-user";
    private final static String UNIX_ACCOUNT_RESOURCE_NAME = "unix-account";

    @Autowired
    public BackupRestController(
            RcUserFeignClient rcUserFeignClient,
            RcStaffFeignClient rcStaffFeignClient
    ) {
        this.rcUserFeignClient = rcUserFeignClient;
        this.rcStaffFeignClient = rcStaffFeignClient;
    }

    @PostMapping("{accountId}/file-backup/restore")
    public ResponseEntity<SimpleServiceMessage> restore(
            @RequestBody SimpleServiceMessage message,
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            SecurityContextHolderAwareRequestWrapper request,
            Authentication authentication
    ) {

        logger.debug("Try restore file from backup " + message.toString());
        String serverName = (String) message.getParam("serverName");
        String snapshotId = (String) message.getParam("snapshotId");
        String pathTo = (String) message.getParam("pathTo");
        String pathFrom = (String) message.getParam("pathFrom");

        if (pathTo == null || pathTo.isEmpty()) {
            pathTo = pathFrom;
        }

        PersonalAccount account = accountManager.findOne(accountId);
        if (!account.isActive()) {
            throw new ParameterValidationException("Аккаунт не активен, восстановление из резервной копии недоступно");
        }

        checkPermission(account, authentication);

        UnixAccount unixAccount = getUnixAccount(accountId);

        checkUnixAccount(unixAccount);
        checkPaths(pathFrom, pathTo, unixAccount);
        checkSnapshotId(snapshotId);

        Server server = getServer(unixAccount);

        SimpleServiceMessage report = new SimpleServiceMessage();
        report.setAccountId(accountId);
        report.addParam("realRoutingKey", getRoutingKeyForTE(server));
        report.setObjRef(String.format("http://%s/%s/%s", RC_USER_APP_NAME, UNIX_ACCOUNT_RESOURCE_NAME, unixAccount.getId()));

        Map<String, String> teParams = new HashMap<>();
        teParams.put(DATASOURCE_URI_KEY, format("restic://%s/%s/%s", serverName, snapshotId, pathFrom));
        teParams.put(DATADESTINATION_URI_KEY, format("file://%s", pathTo));
        report.addParam(TE_PARAMS_KEY, teParams);

        ProcessingBusinessAction action = businessHelper.buildActionAndOperation(
                BusinessOperationType.FILE_BACKUP_RESTORE, FILE_BACKUP_RESTORE_TE, report);

        history.save(
                accountId,
                "Поступила заявка на восстановление из резервной копии (snapshotId: "
                        + message.toString() + ")", request
        );

        return ResponseEntity.accepted().body(createSuccessResponse(action));
    }

    private void checkPermission(PersonalAccount account, Authentication authentication) {
        if (!account.isActive()) {
            throw new ParameterValidationException("Аккаунт неактивен. Восстановление из резервной копии невозможно.");
        }
    }

    private String getRoutingKeyForTE(Server server) {
        return "te." + server.getName().split("\\.")[0];
    }

    private void checkSnapshotId(String snapshotId) throws ParameterValidationException {
        if (snapshotId == null || snapshotId.isEmpty()) {
            String errorMessage = "Необходимо указать id резервной копии в поле snapshotId";
            throw new ParameterValidationException(errorMessage);
        }
    }

    private void checkPaths(String pathFrom, String pathTo, UnixAccount unixAccount) throws ParameterValidationException {
        if (pathFrom == null || pathFrom.isEmpty() || !isInsideInRootDir(pathFrom, unixAccount.getHomeDir())) {
            String errorMessage = "Параметр 'pathFrom' должен находиться внутри домашней директории unixAccount'а " + unixAccount.getHomeDir();
            throw new ParameterValidationException(errorMessage);
        }

        if (pathTo == null || pathTo.isEmpty() || !isInsideInRootDir(pathTo, unixAccount.getHomeDir())) {
            String errorMessage = "Параметр 'pathTo' должен находиться внутри домашней директории unixAccount'а " + unixAccount.getHomeDir();
            throw new ParameterValidationException(errorMessage);
        }
    }

    private Server getServer(UnixAccount unixAccount) throws ResourceNotFoundException {
        try {
            return rcStaffFeignClient.getServerById(unixAccount.getServerId());
        } catch (Exception ignore) {
            logger.error("Не найден сервер с id " + unixAccount.getServerId() + " message: " + ignore.getMessage());
            throw new ParameterValidationException("Не найден сервер unixAccount'а");
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
}
