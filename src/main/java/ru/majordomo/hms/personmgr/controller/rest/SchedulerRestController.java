package ru.majordomo.hms.personmgr.controller.rest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import ru.majordomo.hms.personmgr.event.account.ProcessQuotaChecksEvent;
import ru.majordomo.hms.personmgr.event.token.CleanTokensEvent;

@RestController
public class SchedulerRestController extends CommonRestController {
    @PreAuthorize("hasRole('ADMIN')")
    @RequestMapping(value = "/scheduler/clean_tokens", method = RequestMethod.POST)
    public ResponseEntity<Void> cleanTokens() {
        publisher.publishEvent(new CleanTokensEvent());

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @RequestMapping(value = "/scheduler/process_quota_checks", method = RequestMethod.POST)
    public ResponseEntity<Void> processQuotaChecks() {
        publisher.publishEvent(new ProcessQuotaChecksEvent());

        return new ResponseEntity<>(HttpStatus.OK);
    }
}
