package ru.majordomo.hms.personmgr.controller.rest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;

import ru.majordomo.hms.personmgr.event.account.*;
import ru.majordomo.hms.personmgr.event.token.CleanTokensEvent;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.manager.BatchJobManager;
import ru.majordomo.hms.personmgr.model.batch.BatchJob;

@RestController
@RefreshScope
public class SchedulerRestController extends CommonRestController {
    private final BatchJobManager batchJobManager;
    private int waitForDeadJobHours;

    public SchedulerRestController(
            BatchJobManager batchJobManager
    ) {
        this.batchJobManager = batchJobManager;
    }

    @Value("${batch_job.wait_for_dead_job_hours}")
    public void setWaitForDeadJobHours(int waitForDeadJobHours) {
        this.waitForDeadJobHours = waitForDeadJobHours;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @RequestMapping(value = "/scheduler/{scheduleAction}", method = RequestMethod.POST)
    public ResponseEntity<Void> processScheduleAction(
            @PathVariable(value = "scheduleAction") String scheduleAction
    ) {
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
                publisher.publishEvent(new ProcessExpiringAbonementsEvent());

                break;
            case "process_abonements_auto_renew":
                publisher.publishEvent(new ProcessAbonementsAutoRenewEvent());

                break;
            case "process_notify_expired_abonements":
                publisher.publishEvent(new ProcessNotifyExpiredAbonementsEvent());

                break;
            case "process_recurrents":
                publisher.publishEvent(new ProcessRecurrentsEvent());

                break;
            case "not_registered_domains_in_cart_notification":
                publisher.publishEvent(new AccountNotifyNotRegisteredDomainsInCart());

                break;
            case "process_bulk_revisium_requests":
                publisher.publishEvent(new AccountNotifyNotRegisteredDomainsInCart());

                break;
            default:
                throw new ParameterValidationException("Неизвестный параметр scheduleAction");
        }

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @RequestMapping(value = "/scheduler/jobs/{scheduleAction}/start", method = RequestMethod.POST)
    public ResponseEntity<BatchJob> startScheduleActionWithBatchJob(
            @PathVariable(value = "scheduleAction") String scheduleAction,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @RequestParam(required = false) LocalDate date
    ) {
        if (date == null) {
            date = LocalDate.now();
        }

        BatchJob batchJob;

        switch (scheduleAction) {
            case "prepare_charges":
                batchJob = batchJobManager.findByRunDateAndTypeOrderByCreatedDesc(date, BatchJob.Type.PREPARE_CHARGES);

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
            case "process_charges":
                batchJob = batchJobManager.findByRunDateAndTypeOrderByCreatedDesc(date, BatchJob.Type.PROCESS_CHARGES);

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
            case "process_error_charges":
                batchJob = batchJobManager.findByRunDateAndTypeOrderByCreatedDesc(date, BatchJob.Type.PROCESS_ERROR_CHARGES);

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
                throw new ParameterValidationException("Неизвестный параметр scheduleAction");
        }

        return new ResponseEntity<>(batchJob, HttpStatus.OK);
    }
}
