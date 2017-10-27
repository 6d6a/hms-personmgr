package ru.majordomo.hms.personmgr.controller.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.majordomo.hms.personmgr.service.scheduler.AccountCheckingService;

import java.util.List;

@RestController
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/checking-service")
public class AccountCheckingServiceRestController {

    private final AccountCheckingService accountCheckingService;

    @Autowired
    public AccountCheckingServiceRestController(
        AccountCheckingService accountCheckingService
    ){
        this.accountCheckingService = accountCheckingService;
    }

    @GetMapping("/account-without-abonement-and-plan")
    public ResponseEntity<List<String>> findAccountIdWithoutPlanServiceAndAbonement(){
            return ResponseEntity.ok(accountCheckingService.findAccountIdWithoutPlanServiceAndAbonement());
    }

    @GetMapping("/account-with-more-than-one-plan")
    public ResponseEntity<List<String>> getAccountIdsWithMoreThanOnePlanService(
            @RequestParam(required = false, defaultValue = "true", value = "active") boolean accountActiveState
    ){
        return ResponseEntity.ok(accountCheckingService.getAccountIdsWithMoreThanOnePlanService(accountActiveState));
    }
}
