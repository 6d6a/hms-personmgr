package ru.majordomo.hms.personmgr.controller.rest;

import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.majordomo.hms.personmgr.dto.ResourceCounter;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.dto.AbonementCounter;
import ru.majordomo.hms.personmgr.dto.PlanCounter;
import ru.majordomo.hms.personmgr.service.StatServiceHelper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;


@RestController
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping({"/stat"})
public class StatServiceRestController {
    private final MongoOperations mongoOperations;
    private final StatServiceHelper statServiceHelper;

    public StatServiceRestController(
            MongoOperations mongoOperations,
            StatServiceHelper statServiceHelper
    ) {
        this.mongoOperations = mongoOperations;
        this.statServiceHelper = statServiceHelper;
    }

    @RequestMapping(value = "/count/all-accounts", method = RequestMethod.GET)
    public ResponseEntity<Long> getAccountCount(
    ) {
        return new ResponseEntity<>(mongoOperations.count(query(where("")), PersonalAccount.class), HttpStatus.OK);
    }

    @RequestMapping(value = "/registration-by-date", method = RequestMethod.GET)
    public ResponseEntity<Long> getAccountCount(
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            @RequestParam(value = "date", required = false) LocalDate date
    ) {

        if (date == null) { date = LocalDate.now(); }

        return new ResponseEntity<>(
                mongoOperations.count(
                        query(where("created")
                                .gte(LocalDateTime.of(date, LocalTime.MIN))
                                .lte(LocalDateTime.of(date, LocalTime.MAX))),
                        PersonalAccount.class
                ), HttpStatus.OK);
    }

    @RequestMapping(value = "/plan", method = RequestMethod.GET)
    public ResponseEntity<List<PlanCounter>> getCountsActiveAccountGroupByPlan() {
        return ResponseEntity.ok(statServiceHelper.getAllPlanCounters());
    }

    @GetMapping("/plan/abonement")
    public ResponseEntity<List<AbonementCounter>> getAbonementCounter(){
        return ResponseEntity.ok(statServiceHelper.getAbonementCounters());
    }

    @GetMapping("/plan/daily")
    public ResponseEntity<List<PlanCounter>> getCountsActiveAccountGroupByDailyPlan() {
        return ResponseEntity.ok(statServiceHelper.getDailyPlanCounters());
    }

    @GetMapping("/account-service")
    public ResponseEntity<List<ResourceCounter>> getActiveAccountServiceCounters() {
        return ResponseEntity.ok(statServiceHelper.getActiveAccountServiceCounters());
    }

    @GetMapping("/account-service/quantity")
    public ResponseEntity<List<ResourceCounter>> getQuantityForActiveAccountService() {
        return ResponseEntity.ok(statServiceHelper.getQuantityForActiveAccountService());
    }
}
