package ru.majordomo.hms.personmgr.controller.rest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

import ru.majordomo.hms.personmgr.dto.ScheduleActionParameters;
import ru.majordomo.hms.personmgr.event.account.*;
import ru.majordomo.hms.personmgr.event.accountOrder.SSLCertificateOrderProcessEvent;
import ru.majordomo.hms.personmgr.event.revisium.ProcessBulkRevisiumRequestEvent;
import ru.majordomo.hms.personmgr.event.task.SendLostClientInfoTaskEvent;
import ru.majordomo.hms.personmgr.event.task.CleanFinishedTaskEvent;
import ru.majordomo.hms.personmgr.event.task.NewTasksExecuteEvent;
import ru.majordomo.hms.personmgr.event.task.SendLostDomainsInfoTaskEvent;
import ru.majordomo.hms.personmgr.event.token.CleanTokensEvent;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.manager.BatchJobManager;
import ru.majordomo.hms.personmgr.model.batch.BatchJob;

import javax.annotation.Nullable;

@RestController
@RefreshScope
public class SchedulerRestController extends CommonRestController {
    private final BatchJobManager batchJobManager;
    private final CacheManager cacheManager;
    private int waitForDeadJobHours;

    public SchedulerRestController(
            BatchJobManager batchJobManager,
            CacheManager cacheManager
    ) {
        this.batchJobManager = batchJobManager;
        this.cacheManager = cacheManager;
    }

    @Value("${batch_job.wait_for_dead_job_hours}")
    public void setWaitForDeadJobHours(int waitForDeadJobHours) {
        this.waitForDeadJobHours = waitForDeadJobHours;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @RequestMapping(value = "/scheduler/{scheduleAction}", method = RequestMethod.POST)
    public ResponseEntity<Void> processScheduleAction(
            @PathVariable(value = "scheduleAction") String scheduleAction,
            @Nullable @RequestBody(required = false) ScheduleActionParameters scheduleActionParameters
    ) {
        logger.info("We got scheduler request with action: {}", scheduleAction);
        
        switch (scheduleAction) {
            case "clean_tokens":
                publisher.publishEvent(new CleanTokensEvent());

                break;
            case "process_quota_checks":
                publisher.publishEvent(new ProcessQuotaChecksEvent());

                break;
            case "process_account_deactivated_send_mail":
                publisher.publishEvent(new ProcessAccountDeactivatedSendMailEvent());

                break;
            case "process_account_deactivated_send_sms":
                publisher.publishEvent(new ProcessAccountDeactivatedSendSmsEvent());

                break;
            case "process_account_no_abonement_send_mail":
                publisher.publishEvent(new ProcessAccountNoAbonementSendMailEvent());

                break;
            case "process_notify_inactive_long_time":
                publisher.publishEvent(new ProcessNotifyInactiveLongTimeEvent());

                break;
            case "process_send_info_mail":
                publisher.publishEvent(new ProcessSendInfoMailEvent());

                break;
            case "process_expiring_domains":
                publisher.publishEvent(new ProcessExpiringDomainsEvent());

                break;
            case "process_domains_auto_renew":
                publisher.publishEvent(new ProcessDomainsAutoRenewEvent());

                break;
            case "clean_business_actions":
                publisher.publishEvent(new CleanBusinessActionsEvent());

                break;
            case "process_expiring_abonements":
                // ?????????? ???????????????????? ???????????????? ?????????????????????? ???? ?????????????????? ?????????????????????? ???? ???????????????? ??????????. ???? ???????????????????? ???????????????? ?? ??????????????????
                publisher.publishEvent(new ProcessExpiringAbonementsEvent());

                break;
            case "process_expiring_service_abonements":
                publisher.publishEvent(new ProcessExpiringServiceAbonementsEvent());

                break;
            case "process_abonements_auto_renew":
                publisher.publishEvent(new ProcessAbonementsAutoRenewEvent());

                break;
            case "process_service_abonements_auto_renew":
                publisher.publishEvent(new ProcessServiceAbonementsAutoRenewEvent());

                break;
            case "process_notify_expired_abonements":
                publisher.publishEvent(new ProcessNotifyExpiredAbonementsEvent());

                break;
            case "process_recurrents":
                publisher.publishEvent(new ProcessRecurrentsEvent());

                break;
            case "process_resource_archives":
                publisher.publishEvent(new ProcessResourceArchivesEvent());

                break;
            case "not_registered_domains_in_cart_notification":
                publisher.publishEvent(new AccountNotifyNotRegisteredDomainsInCart());

                break;
            case "process_bulk_revisium_requests":
                publisher.publishEvent(new ProcessBulkRevisiumRequestEvent());

                break;
            case "process_notify_expiring_bitrix_license":
                publisher.publishEvent(new ProcessNotifyExpiringBitrixLicenseEvent());

                break;
            case "process_deferred_plan_change":
                publisher.publishEvent(new DeferredPlanChangeEvent());

                break;
            case "process_charge_inactive_account_long_time":
                publisher.publishEvent(new ProcessChargeInactiveAccountEvent());

                break;
            case "process_delete_data_inactive_accounts":
                publisher.publishEvent(new ProcessDeleteDataInactiveAccountsEvent());

                break;
            case "process_execute_tasks":
                publisher.publishEvent(new NewTasksExecuteEvent());

                break;
            case "process_clean_tasks":
                publisher.publishEvent(new CleanFinishedTaskEvent());

                break;
            case "process_send_lost_client_info":
                publisher.publishEvent(new SendLostClientInfoTaskEvent());

                break;
            case "process_send_lost_domains_info":
                publisher.publishEvent(new SendLostDomainsInfoTaskEvent());

                break;
            case "process_ssl_order":
                publisher.publishEvent(new SSLCertificateOrderProcessEvent());

                break;
            case "process_buy_preorders":
                publisher.publishEvent(new AttemptBuyPreordersEvent());
                break;

            case "process_plan_daily_diagnostic":
                // ?????????? ?????????????????? ?? ?????????????????????????? ?????????????????? ??????????????, ???????????????? ?? ????????????????????????
                PlanDailyDiagnosticEvent event;
                if (scheduleActionParameters != null) {
                    event = new PlanDailyDiagnosticEvent(
                            scheduleActionParameters.isSkipAlerta(),
                            scheduleActionParameters.isIncludeInactive(),
                            scheduleActionParameters.isSearchAbonementWithoutExpired()
                    );
                } else {
                    event = new PlanDailyDiagnosticEvent();
                }
                publisher.publishEvent(event);
                break;

            default:
                throw new ParameterValidationException("?????????????????????? ???????????????? scheduleAction");
        }

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @RequestMapping(value = "/scheduler/jobs/{scheduleAction}/start", method = RequestMethod.POST)
    public ResponseEntity<BatchJob> startScheduleActionWithBatchJob(
            @PathVariable(value = "scheduleAction") String scheduleAction,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @RequestParam(required = false) LocalDate date
    ) {
        logger.info("We got scheduler request with action: {}, date: {}", scheduleAction, date == null ? "null": date);

        if (date == null) {
            date = LocalDate.now();
        }

        BatchJob batchJob;

        switch (scheduleAction) {
            case "prepare_charges": // ???????????????????? ?????????????? ???? ???????????????????? ???????????????? ???? ????????????
                batchJob = batchJobManager.findFirstByRunDateAndTypeOrderByCreatedDesc(date, BatchJob.Type.PREPARE_CHARGES);

                if (batchJob == null ||
                        (batchJob.getUpdated().isBefore(LocalDateTime.now().minusHours(waitForDeadJobHours)) &&
                                batchJob.getState() != BatchJob.State.FINISHED)) {
                    batchJob = new BatchJob();
                    batchJob.setRunDate(date);
                    batchJob.setType(BatchJob.Type.PREPARE_CHARGES);

                    batchJobManager.insert(batchJob);

                    publisher.publishEvent(new PrepareChargesEvent(date, batchJob.getId()));
                }

                break;
            case "process_charges": // ?????????????????? ?????????????? ???? ???????????????????? ???????????????? ???? ????????????
                batchJob = batchJobManager.findFirstByRunDateAndTypeOrderByCreatedDesc(date, BatchJob.Type.PROCESS_CHARGES);

                if (batchJob == null ||
                        (batchJob.getUpdated().isBefore(LocalDateTime.now().minusHours(waitForDeadJobHours)) &&
                                batchJob.getState() != BatchJob.State.FINISHED)) {
                    batchJob = new BatchJob();
                    batchJob.setRunDate(date);
                    batchJob.setType(BatchJob.Type.PROCESS_CHARGES);

                    batchJobManager.insert(batchJob);

                    publisher.publishEvent(new ProcessChargesEvent(date, batchJob.getId()));
                }

                break;
            case "process_error_charges": // ?????????????????? ?????????????????????? ?????????????? ???? ???????????????????? ???????????????? ???? ????????????
                batchJob = batchJobManager.findFirstByRunDateAndTypeOrderByCreatedDesc(date, BatchJob.Type.PROCESS_ERROR_CHARGES);

                if (batchJob == null ||
                        (batchJob.getUpdated().isBefore(LocalDateTime.now().minusHours(waitForDeadJobHours))
                                && batchJob.getState() != BatchJob.State.FINISHED)) {
                    batchJob = new BatchJob();
                    batchJob.setRunDate(date);
                    batchJob.setType(BatchJob.Type.PROCESS_ERROR_CHARGES);

                    batchJobManager.insert(batchJob);

                    publisher.publishEvent(new ProcessErrorChargesEvent(date, batchJob.getId()));
                }

                break;
            default:
                throw new ParameterValidationException("?????????????????????? ???????????????? scheduleAction");
        }

        return new ResponseEntity<>(batchJob, HttpStatus.OK);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @RequestMapping(value = "/cache-clear", method = RequestMethod.POST)
    public ResponseEntity<Void> processScheduleAction() {
        cacheManager.getCacheNames()
                .stream()
                .map(cacheManager::getCache)
                .forEach(Cache::clear);

        return new ResponseEntity<>(HttpStatus.OK);
    }
}
