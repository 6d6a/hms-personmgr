package ru.majordomo.hms.personmgr.controller.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestWrapper;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.majordomo.hms.personmgr.common.StorageType;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.dto.request.FileRestoreRequest;
import ru.majordomo.hms.personmgr.dto.request.MysqlRestoreRequest;
import ru.majordomo.hms.personmgr.dto.request.RestoreRequest;
import ru.majordomo.hms.personmgr.service.BackupService;
import ru.majordomo.hms.personmgr.service.restic.Snapshot;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.validation.ObjectId;

import javax.validation.Valid;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@Validated
public class BackupRestController extends CommonRestController{

    private BackupService backupService;

    @Autowired
    public BackupRestController(
            BackupService backupService
    ) {
        this.backupService = backupService;
    }

    @GetMapping("/{accountId}/backup")
    public List<Snapshot> getSnapshots(
            @ObjectId(PersonalAccount.class) @PathVariable String accountId,
            @RequestParam StorageType type
    ) {
        PersonalAccount account = accountManager.findOne(accountId);
        if (!account.isActive()) {
            return Collections.emptyList();
        }

        List<Snapshot> snapshots;
        switch (type) {
            case FILE:
                snapshots = backupService.getFileSnapshots(account);

                break;
            case MYSQL:
                snapshots = backupService.getDbSnapshots(account);

                break;
            default:
                throw new ParameterValidationException("Неизвестный тип восстановления");
        }

        LocalDate minTimeForBackup = backupService.minDateForBackup(account, type);

        return snapshots
                .stream()
                .peek(item -> {
                    if (item.getTime().toLocalDate().isAfter(minTimeForBackup)) {
                        item.setHidden(false);
                    }
                })
                .collect(Collectors.toList());
    }

    @PostMapping("/{accountId}/backup/restore")
    public ResponseEntity<SimpleServiceMessage> restore(
            @Valid @RequestBody RestoreRequest restoreRequest,
            @ObjectId(PersonalAccount.class) @PathVariable String accountId,
            SecurityContextHolderAwareRequestWrapper request
    ) {
        logger.info("Try restore from backup accountId: {} {}", accountId, restoreRequest.toString());

        SimpleServiceMessage result;

        PersonalAccount account = accountManager.findOne(accountId);
        assertAccountIsActive(account);

        if (restoreRequest instanceof MysqlRestoreRequest) {
            result = backupService.restoreMysql(account, (MysqlRestoreRequest) restoreRequest, request);
        } else if (restoreRequest instanceof FileRestoreRequest) {
            result = backupService.restoreFileBackup(account, (FileRestoreRequest) restoreRequest, request);
        } else {
            throw new ParameterValidationException("Неизвестный тип восстановления");
        }

        return ResponseEntity.accepted().body(result);
    }

    private void assertAccountIsActive(PersonalAccount account) {
        if (!account.isActive()) {
            throw new ParameterValidationException("Аккаунт не активен");
        }
    }
}
