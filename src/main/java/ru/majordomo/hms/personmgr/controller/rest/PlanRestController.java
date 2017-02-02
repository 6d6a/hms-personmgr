package ru.majordomo.hms.personmgr.controller.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.repository.PlanRepository;

@RestController
@RequestMapping({"/{accountId}/plans", "/plans"})
public class PlanRestController extends CommonRestController {

    private final PlanRepository repository;

    @Autowired
    public PlanRestController(PlanRepository repository) {
        this.repository = repository;
    }

    @RequestMapping(value = "", method = RequestMethod.GET)
    public ResponseEntity<List<Plan>> listAll(@PathVariable(value = "accountId", required = false) String accountId) {
        List<Plan> plans = repository.findByActive(true);
        if(plans.isEmpty()){
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(plans, HttpStatus.OK);
    }
}