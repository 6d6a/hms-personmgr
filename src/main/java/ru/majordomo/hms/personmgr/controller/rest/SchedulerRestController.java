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

import ru.majordomo.hms.personmgr.event.account.CleanBusinessActionsEvent;
import ru.majordomo.hms.personmgr.event.account.PrepareChargesEvent;
import ru.majordomo.hms.personmgr.event.account.ProcessAbonementsAutoRenewEvent;
import ru.majordomo.hms.personmgr.event.account.ProcessAccountDeactivatedSendMailEvent;
import ru.majordomo.hms.personmgr.event.account.ProcessChargesEvent;
import ru.majordomo.hms.personmgr.event.account.ProcessDomainsAutoRenewEvent;
import ru.majordomo.hms.personmgr.event.account.ProcessExpiringAbonementsEvent;
import ru.majordomo.hms.personmgr.event.account.ProcessExpiringDomainsEvent;
import ru.majordomo.hms.personmgr.event.account.ProcessNotifyExpiredAbonementsEvent;
import ru.majordomo.hms.personmgr.event.account.ProcessNotifyInactiveLongTimeEvent;
import ru.majordomo.hms.personmgr.event.account.ProcessQuotaChecksEvent;
import ru.majordomo.hms.personmgr.event.account.ProcessRecurrentsEvent;
import ru.majordomo.hms.personmgr.event.account.ProcessSendInfoMailEvent;
import ru.majordomo.hms.personmgr.event.token.CleanTokensEvent;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;

@RestController
public class SchedulerRestController extends CommonRestController {
    @PreAuthorize("hasRole('ADMIN')")
    @RequestMapping(value = "/scheduler/{scheduleAction}", method = RequestMethod.POST)
    public ResponseEntity<Void> processScheduleAction(
            @PathVariable(value = "scheduleAction") String scheduleAction,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @RequestParam(required = false) LocalDate date
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
            case "prepare_charges":
                if (date != null) {
                    publisher.publishEvent(new PrepareChargesEvent(date));
                } else {
                    publisher.publishEvent(new PrepareChargesEvent());
                }

                break;
            case "process_charges":
                if (date != null) {
                    publisher.publishEvent(new ProcessChargesEvent(date));
                } else {
                    publisher.publishEvent(new ProcessChargesEvent());
                }

                break;
            default:
                throw new ParameterValidationException("Неизвестный параметр scheduleAction");
        }

        return new ResponseEntity<>(HttpStatus.OK);
    }
}
