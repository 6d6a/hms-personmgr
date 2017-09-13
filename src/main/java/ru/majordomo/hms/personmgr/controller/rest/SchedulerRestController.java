package ru.majordomo.hms.personmgr.controller.rest;

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

import ru.majordomo.hms.personmgr.event.account.CleanBusinessActionsEvent;
import ru.majordomo.hms.personmgr.event.account.PrepareChargesEvent;
import ru.majordomo.hms.personmgr.event.account.ProcessAbonementsAutoRenewEvent;
import ru.majordomo.hms.personmgr.event.account.ProcessAccountDeactivatedSendMailEvent;
import ru.majordomo.hms.personmgr.event.account.ProcessChargesEvent;
import ru.majordomo.hms.personmgr.event.account.ProcessDomainsAutoRenewEvent;
import ru.majordomo.hms.personmgr.event.account.ProcessErrorChargesEvent;
import ru.majordomo.hms.personmgr.event.account.ProcessExpiringAbonementsEvent;
import ru.majordomo.hms.personmgr.event.account.ProcessExpiringDomainsEvent;
import ru.majordomo.hms.personmgr.event.account.ProcessNotifyExpiredAbonementsEvent;
import ru.majordomo.hms.personmgr.event.account.ProcessNotifyInactiveLongTimeEvent;
import ru.majordomo.hms.personmgr.event.account.ProcessQuotaChecksEvent;
import ru.majordomo.hms.personmgr.event.account.ProcessRecurrentsEvent;
import ru.majordomo.hms.personmgr.event.account.ProcessSendInfoMailEvent;
import ru.majordomo.hms.personmgr.event.token.CleanTokensEvent;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.manager.BatchJobManager;
import ru.majordomo.hms.personmgr.model.batch.BatchJob;
import ru.majordomo.hms.personmgr.service.ChargePreparer;
import ru.majordomo.hms.personmgr.service.ChargeProcessor;

@RestController
public class SchedulerRestController extends CommonRestController {
    private final ChargePreparer chargePreparer;
    private final ChargeProcessor chargeProcessor;
    private final BatchJobManager batchJobManager;

    public SchedulerRestController(
            ChargePreparer chargePreparer,
            ChargeProcessor chargeProcessor,
            BatchJobManager batchJobManager
    ) {
        this.chargePreparer = chargePreparer;
        this.chargeProcessor = chargeProcessor;
        this.batchJobManager = batchJobManager;
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
                batchJob = batchJobManager.findByRunDateAndTypeOrderByCreatedAsc(date, BatchJob.Type.PREPARE_CHARGES);

                if (batchJob == null || (batchJob.getUpdated().isBefore(LocalDateTime.now().minusHours(2)) && batchJob.getState() != BatchJob.State.FINISHED)) {
                    batchJob = new BatchJob();
                    batchJob.setRunDate(date);
                    batchJob.setType(BatchJob.Type.PREPARE_CHARGES);

                    batchJobManager.insert(batchJob);

                    publisher.publishEvent(new PrepareChargesEvent(date, batchJob.getId()));
                }

                break;
            case "process_charges":
                batchJob = batchJobManager.findByRunDateAndTypeOrderByCreatedAsc(date, BatchJob.Type.PROCESS_CHARGES);

                if (batchJob == null || (batchJob.getUpdated().isBefore(LocalDateTime.now().minusHours(2)) && batchJob.getState() != BatchJob.State.FINISHED)) {
                    batchJob = new BatchJob();
                    batchJob.setRunDate(date);
                    batchJob.setType(BatchJob.Type.PROCESS_CHARGES);

                    batchJobManager.insert(batchJob);

                    publisher.publishEvent(new ProcessChargesEvent(date, batchJob.getId()));
                }

                break;
            case "process_error_charges":
                batchJob = batchJobManager.findByRunDateAndTypeOrderByCreatedAsc(date, BatchJob.Type.PROCESS_ERROR_CHARGES);

                if (batchJob == null || (batchJob.getUpdated().isBefore(LocalDateTime.now().minusHours(2)) && batchJob.getState() != BatchJob.State.FINISHED)) {
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
