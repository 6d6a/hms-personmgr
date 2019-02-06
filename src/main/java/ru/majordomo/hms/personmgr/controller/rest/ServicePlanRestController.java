package ru.majordomo.hms.personmgr.controller.rest;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Predicate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.querydsl.binding.QuerydslPredicate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import ru.majordomo.hms.personmgr.model.plan.QServicePlan;
import ru.majordomo.hms.personmgr.model.plan.ServicePlan;
import ru.majordomo.hms.personmgr.repository.ServicePlanRepository;
import ru.majordomo.hms.personmgr.service.ServicePlanBuilder;


@RestController
public class ServicePlanRestController extends CommonRestController {
    private final ServicePlanRepository repository;
    private final ServicePlanBuilder servicePlanBuilder;

    @Autowired
    public ServicePlanRestController(
            ServicePlanRepository repository,
            ServicePlanBuilder servicePlanBuilder
    ) {
        this.repository = repository;
        this.servicePlanBuilder = servicePlanBuilder;
    }

    @GetMapping("/{accountId}/service-plans")
    public ResponseEntity<List<ServicePlan>> listAllForAccount(
            @PathVariable(value = "accountId") String accountId,
            @QuerydslPredicate(root = ServicePlan.class) Predicate predicate
    ) {
        QServicePlan qServicePlan = QServicePlan.servicePlan;
        BooleanBuilder builder = new BooleanBuilder();
        Predicate defaultPredicate= builder
                .and(qServicePlan.active.isTrue());

        if(predicate == null) {
            predicate = defaultPredicate;
        } else {
            predicate = ExpressionUtils.allOf(predicate, defaultPredicate);
        }

        List<ServicePlan> servicePlans = (List<ServicePlan>) repository.findAll(predicate);

        servicePlans.forEach(servicePlanBuilder::build);

        return new ResponseEntity<>(servicePlans, HttpStatus.OK);
    }

    @GetMapping("/service-plans")
    public ResponseEntity<Page<ServicePlan>> listAll(
            @QuerydslPredicate(root = ServicePlan.class) Predicate predicate,
            Pageable pageable) {
        if (predicate == null) predicate = new BooleanBuilder();

        Page<ServicePlan> servicePlans = repository.findAll(predicate, pageable);

        servicePlans.forEach(servicePlanBuilder::build);

        return new ResponseEntity<>(servicePlans, HttpStatus.OK);
    }
}