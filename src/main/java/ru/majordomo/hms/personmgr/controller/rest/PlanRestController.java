package ru.majordomo.hms.personmgr.controller.rest;

import com.querydsl.core.types.Predicate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.querydsl.binding.QuerydslPredicate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import ru.majordomo.hms.personmgr.manager.PlanManager;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.service.PlanBuilder;


@RestController
public class PlanRestController extends CommonRestController {
    private final PlanManager manager;
    private final PlanBuilder planBuilder;

    @Autowired
    public PlanRestController(PlanManager manager, PlanBuilder planBuilder) {
        this.manager = manager;
        this.planBuilder = planBuilder;
    }

    @GetMapping("/{accountId}/plans")
    public ResponseEntity<List<Plan>> listAllForAccount(@PathVariable(value = "accountId") String accountId) {
        List<Plan> plans = manager.findByActive(true);

        return new ResponseEntity<>(plans, HttpStatus.OK);
    }

    @GetMapping("/plans")
    public ResponseEntity<Page<Plan>> listAll(
            @QuerydslPredicate(root = Plan.class) Predicate predicate,
            Pageable pageable) {
        Page<Plan> plans = manager.findAll(predicate, pageable);

        plans.forEach(planBuilder::build);

        return new ResponseEntity<>(plans, HttpStatus.OK);
    }

    @GetMapping("/plans/{planId}")
    public ResponseEntity<Plan> get(@PathVariable(value = "planId") String planId) {
        Plan plan = manager.findOne(planId);

        return new ResponseEntity<>(plan, HttpStatus.OK);
    }

    @GetMapping("/plans/serviceId/{serviceId}")
    public ResponseEntity<Plan> getByServiceId(@PathVariable String serviceId){
        return new ResponseEntity<>(manager.findByServiceId(serviceId), HttpStatus.OK);
    }
}