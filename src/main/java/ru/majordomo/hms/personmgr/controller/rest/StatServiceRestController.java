package ru.majordomo.hms.personmgr.controller.rest;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.majordomo.hms.personmgr.common.AccountStatType;
import ru.majordomo.hms.personmgr.common.BusinessOperationType;
import ru.majordomo.hms.personmgr.common.State;
import ru.majordomo.hms.personmgr.dto.stat.*;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.service.StatServiceHelper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

@Slf4j
@RestController
@RequestMapping({"/stat"})
public class StatServiceRestController {
    private final MongoOperations mongoOperations;
    private final StatServiceHelper statServiceHelper;

    private enum DomainActionType {
        register, manualrenew, autorenew
    }

    public StatServiceRestController(
            MongoOperations mongoOperations,
            StatServiceHelper statServiceHelper
    ) {
        this.mongoOperations = mongoOperations;
        this.statServiceHelper = statServiceHelper;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @RequestMapping(value = "/count/all-accounts", method = RequestMethod.GET)
    public ResponseEntity<Long> getAccountCount(
    ) {
        return new ResponseEntity<>(mongoOperations.count(query(where("")), PersonalAccount.class), HttpStatus.OK);
    }

    @PreAuthorize("hasRole('ADMIN')")
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

    @PreAuthorize("hasRole('ADMIN')")
    @RequestMapping(value = "/plan", method = RequestMethod.GET)
    public ResponseEntity<List<PlanCounter>> getCountsActiveAccountGroupByPlan() {
        return ResponseEntity.ok(statServiceHelper.getAllPlanCounters());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/plan/abonement")
    public ResponseEntity<List<AbonementCounter>> getAbonementCounter(){
        return ResponseEntity.ok(statServiceHelper.getAbonementCounters());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/plan/daily")
    public ResponseEntity<List<PlanCounter>> getCountsActiveAccountGroupByDailyPlan() {
        return ResponseEntity.ok(statServiceHelper.getDailyPlanCounters());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/account-service")
    public ResponseEntity<List<AccountServiceCounter>> getActiveAccountServiceCounters() {
        return ResponseEntity.ok(statServiceHelper.getActiveAccountServiceCounters());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/domain")
    public ResponseEntity<List<DomainCounter>> getDomainCountersByType(
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            @RequestParam(required = false) LocalDate start,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            @RequestParam(required = false) LocalDate end,
            @RequestParam DomainActionType type
    ) {

        AccountStatType statType = getAccountStatTypeFromDomainActionType(type);
        if (statType == null) { return ResponseEntity.badRequest().build(); }

        if (end == null) { end = LocalDate.now(); }
        if (start == null) { start = end.minusDays(1); }

        List<DomainCounter> result = new ArrayList<>();

        while (start.isBefore(end)) {
            result.addAll(statServiceHelper.getDomainCountersByDateAndStatType(start, statType));
            start = start.plusDays(1);
        }
        return ResponseEntity.ok(result);
    }

    private AccountStatType getAccountStatTypeFromDomainActionType(DomainActionType type) {
        switch (type){
            case manualrenew:
                return AccountStatType.VIRTUAL_HOSTING_MANUAL_RENEW_DOMAIN;
            case autorenew:
                return AccountStatType.VIRTUAL_HOSTING_AUTO_RENEW_DOMAIN;
            case register:
                return AccountStatType.VIRTUAL_HOSTING_REGISTER_DOMAIN;
            default:
                return null;
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/first-payment")
    public ResponseEntity<Integer> getAccountCountsWithFirstRealPaymentByDate(
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            @RequestParam(required = false) LocalDate date
    ) {
        if (date == null) { date = LocalDate.now().minusDays(1); }

        return ResponseEntity.ok(statServiceHelper.getAccountCountsWithFirstRealPaymentByDate(date));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/register-with-plan")
    public ResponseEntity<List<ResourceCounter>> getRegisterWithPlanCounters(
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            @RequestParam(required = false) LocalDate date
    ){
        if (date == null) { date = LocalDate.now().minusDays(1); }

        return ResponseEntity.ok(statServiceHelper.getRegisterWithPlanCounters(date));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/processing-business-operation")
    public ResponseEntity<List<Map>> processingBusinessOperationsStatistic(
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @RequestParam LocalDate start,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @RequestParam LocalDate end,
            @RequestParam Set<State> states,
            @RequestParam Set<BusinessOperationType> types
    ){
        return ResponseEntity.ok(statServiceHelper.getBusinessOperationsStat(types, states, start, end));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/service-abonement")
    public List<AccountServiceCounter> serviceAbonement(){
        return statServiceHelper.getServiceAbonementCounters();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/tariff-by-server")
    public List<PlanByServerCounter> getTariffByServer() {
        return statServiceHelper.getPlanByServerStat();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'STAT')")
    @GetMapping("/meta/options")
    public List<Options> getOptions() {
        return statServiceHelper.getMetaOptions();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'STAT')")
    @GetMapping("/meta/registration")
    public Collection<MetaProjection> getMetaRegistrations(
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @RequestParam LocalDate start,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @RequestParam LocalDate end,
            @RequestParam Map<String, String> search
    ) {
        search.remove("start");
        search.remove("end");

        return statServiceHelper.getMetaStat(start, end, search);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/account-promotion")
    public ResponseEntity<List<AccountPromotionCounter>> getAccountPromotionStat(
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            @RequestParam(required = false) LocalDate date,
            @RequestParam("active") boolean active
    ) {
        if (date == null) { date = LocalDate.now().minusDays(1); }

        return ResponseEntity.ok(statServiceHelper.getAccountPromotionStat(date, active));
    }
}
