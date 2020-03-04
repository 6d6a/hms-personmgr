package ru.majordomo.hms.personmgr.controller.rest;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang.StringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestWrapper;
import org.springframework.web.bind.annotation.*;
import ru.majordomo.hms.personmgr.common.BusinessOperationType;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.dto.Result;
import ru.majordomo.hms.personmgr.dto.importing.BillingDBAccountStatus;
import ru.majordomo.hms.personmgr.event.account.DBImportEvent;
import ru.majordomo.hms.personmgr.exception.*;
import ru.majordomo.hms.personmgr.feign.RcStaffFeignClient;
import ru.majordomo.hms.personmgr.importing.DBImportService;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessOperation;
import ru.majordomo.hms.rc.staff.resources.Server;

import java.util.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/import")
@PreAuthorize("hasRole('ADMIN') or hasRole('SUPPORT')")
public class ImportRestController extends CommonRestController {
    private final DBImportService dbImportService;
    private final RcStaffFeignClient rcStaffFeignClient;

    private final static String NOT_HMS_SERVER = "web_server_53";

    @PostMapping
    public ResponseEntity<ProcessingBusinessOperation> importToMongo(
            @RequestBody SimpleServiceMessage message,
            SecurityContextHolderAwareRequestWrapper request
    ) {
        try {
            String accountId = StringUtils.defaultString(message.getAccountId());
            String serverId = StringUtils.defaultString((String) message.getParam("serverId"));

            BillingDBAccountStatus status = dbImportService.getAccountStatus(accountId, request.isUserInRole("ADMIN"));
            if (status == null) {
                throw new ResourceNotFoundException();
            }

            if (status.isOnHms()) {
                throw new ParameterValidationException("Импорт находящихся в HMS аккаунтов запрещен");
            }

            if (!status.isAllowImport()) {
                throw new ParameterValidationException("Импорт аккаунта запрещен");
            }


            if (serverId.equals(status.getHmsServerId())) {
                throw new InternalApiException("Импорт аккаунта на тот же сервер запрещен");
            }

            if (StringUtils.isNotEmpty(serverId)) {
                if (NOT_HMS_SERVER.equals(serverId)) {
                    throw new ParameterValidationException("Неправильный идентификатор сервера");
                }
                Server server = rcStaffFeignClient.getServerById(serverId);
                if (server == null || server.getServerRoles().stream().noneMatch(serverRole -> "shared-hosting".equals(serverRole.getName()))) {
                    throw new ParameterValidationException("Неправильный идентификатор сервера");
                }
            } else if (NOT_HMS_SERVER.equals(status.getHmsServerId()) || StringUtils.isEmpty(status.getHmsServerId())) {
                throw new ParameterValidationException("Необходимо задать сервер на котором будет находиться аккаунт");
            }

            String mysqlServiceId = dbImportService.getMysqlServiceId(serverId);

            message.addParam("stage", DBImportService.ImportStage.CREATED);
            message.addParam("mysqlServiceId", mysqlServiceId);

            ProcessingBusinessOperation operation = lockAndBuildOperation(message);

            publisher.publishEvent(new DBImportEvent(accountId, serverId, operation.getId(), mysqlServiceId));

            return ResponseEntity.accepted().body(operation);
        } catch (FeignException ex) {
            logger.error("Exception when send request " + message.getAccountId(), ex);
            throw new InternalApiException();
        } catch (RuntimeException ex) {
            logger.error("Exception when import account " + message.getAccountId(), ex);
            throw ex instanceof BaseException ? ex : new InternalApiException("Exception when import: " + ex.toString());
        }
    }

    private ProcessingBusinessOperation lockAndBuildOperation(SimpleServiceMessage message) throws ResourceIsLockedException {
        ProcessingBusinessOperation operation = businessHelper.buildOperationAtomic(BusinessOperationType.IMPORT_FROM_BILLINGDB, message,
                Collections.singletonMap("serverId", message.getParam("serverId")));
        if (operation == null) {
            throw new ResourceIsLockedException("Перенос уже выполняется");
        } else {
            return operation;
        }
    }

    @GetMapping("{accountId}")
    public ResponseEntity<BillingDBAccountStatus> getStatus(
            @PathVariable String accountId,
            SecurityContextHolderAwareRequestWrapper request
    ) {
        BillingDBAccountStatus status = dbImportService.getAccountStatus(accountId, request.isUserInRole("ADMIN"));
        if (status != null) {
            return ResponseEntity.ok(status);
        } else {
            throw new ResourceNotFoundException(String.format("Аккаунт %s не найден", accountId));
        }
    }

    @PostMapping("{accountId}/on_hms")
    public ResponseEntity<Result> setOnHms(
            @PathVariable String accountId,
            @RequestParam boolean onHms,
            SecurityContextHolderAwareRequestWrapper request
    ) {
        dbImportService.setOnHms(accountId, onHms, request.isUserInRole("ADMIN")); // сменит или бросит правильное исключение с текстом
        return ResponseEntity.ok(Result.success());
    }
}
